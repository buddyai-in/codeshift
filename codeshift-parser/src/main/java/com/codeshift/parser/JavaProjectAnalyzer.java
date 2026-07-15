package com.codeshift.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Discovery Agent core (deterministic). Scans a directory of Java sources with
 * JavaParser and produces a {@link ProjectAnalysis}: module inventory, an
 * inter-module dependency graph derived from imports, detected messaging systems,
 * and a pre-Jakarta ({@code javax.*}) migration signal.
 *
 * <p>Deterministic on purpose — no LLM. This is the "deterministic first" lever:
 * the dependency graph and translation order are computed exactly, not guessed.
 */
public final class JavaProjectAnalyzer {

    private static final Map<String, String> MESSAGING_MARKERS = Map.of(
            "javax.jms", "JMS",
            "jakarta.jms", "JMS",
            "com.ibm.mq", "IBM_MQ",
            "com.ibm.msg", "IBM_MQ",
            "org.apache.activemq", "ACTIVEMQ",
            "com.rabbitmq", "RABBITMQ",
            "org.springframework.amqp", "RABBITMQ",
            "org.apache.kafka", "KAFKA",
            "org.springframework.kafka", "KAFKA");

    private static final Set<String> WEB_ENTRY_ANNOTATIONS =
            Set.of("RestController", "Controller", "SpringBootApplication", "WebServlet");

    private JavaProjectAnalyzer() {}

    public static ProjectAnalysis analyze(Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Not a directory: " + root);
        }
        JavaParser parser = new JavaParser(
                new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_21));

        List<SourceModule> modules = new ArrayList<>();
        Set<String> messaging = new LinkedHashSet<>();
        boolean[] usesJavax = {false};

        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().endsWith("package-info.java"))
                    .sorted()
                    .forEach(p -> parseFile(parser, root, p, modules, messaging, usesJavax));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk " + root, e);
        }

        return new ProjectAnalysis(modules, buildEdges(modules), messaging, usesJavax[0]);
    }

    private static void parseFile(JavaParser parser, Path root, Path file,
            List<SourceModule> modules, Set<String> messaging, boolean[] usesJavax) {
        try {
            ParseResult<CompilationUnit> result = parser.parse(file);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                return; // skip unparseable files — a legacy codebase always has a few
            }
            CompilationUnit cu = result.getResult().get();
            String pkg = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString()).orElse("");

            List<String> imports = new ArrayList<>();
            cu.getImports().forEach(imp -> {
                String name = imp.getNameAsString();
                imports.add(imp.isAsterisk() ? name + ".*" : name);
                if (name.startsWith("javax.")) {
                    usesJavax[0] = true;
                }
                MESSAGING_MARKERS.forEach((marker, system) -> {
                    if (name.startsWith(marker)) {
                        messaging.add(system);
                    }
                });
            });

            int loc = countLoc(file);
            for (TypeDeclaration<?> type : cu.getTypes()) {
                if (!type.isTopLevelType()) {
                    continue;
                }
                String simple = type.getNameAsString();
                String id = pkg.isEmpty() ? simple : pkg + "." + simple;
                modules.add(new SourceModule(id, simple, pkg,
                        root.relativize(file).toString(), loc, isEntryPoint(type), imports));
            }
        } catch (IOException e) {
            // Unreadable file — skip rather than fail the whole assessment.
        }
    }

    private static boolean isEntryPoint(TypeDeclaration<?> type) {
        boolean hasMain = type.findAll(MethodDeclaration.class).stream()
                .anyMatch(m -> m.getNameAsString().equals("main")
                        && m.isStatic() && m.isPublic());
        boolean webEntry = WEB_ENTRY_ANNOTATIONS.stream().anyMatch(a ->
                type.getAnnotationByName(a).isPresent());
        return hasMain || webEntry;
    }

    private static int countLoc(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file)) {
            return (int) lines.filter(l -> !l.isBlank()).count();
        }
    }

    /** Edges from explicit imports that resolve to another module in the project. */
    private static List<String[]> buildEdges(List<SourceModule> modules) {
        Map<String, SourceModule> byId = new LinkedHashMap<>();
        Map<String, List<SourceModule>> byPackage = new LinkedHashMap<>();
        for (SourceModule m : modules) {
            byId.put(m.id(), m);
            byPackage.computeIfAbsent(m.packageName(), k -> new ArrayList<>()).add(m);
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String[]> edges = new ArrayList<>();
        for (SourceModule m : modules) {
            for (String imp : m.imports()) {
                if (imp.endsWith(".*")) {
                    String pkg = imp.substring(0, imp.length() - 2);
                    for (SourceModule t : byPackage.getOrDefault(pkg, List.of())) {
                        addEdge(edges, seen, m, t);
                    }
                } else {
                    SourceModule t = byId.get(imp);
                    if (t != null) {
                        addEdge(edges, seen, m, t);
                    }
                }
            }
        }
        return edges;
    }

    private static void addEdge(List<String[]> edges, Set<String> seen,
            SourceModule from, SourceModule to) {
        if (from.id().equals(to.id())) {
            return;
        }
        String key = from.id() + "|" + to.id();
        if (seen.add(key)) {
            edges.add(new String[] {from.id(), to.id()});
        }
    }
}
