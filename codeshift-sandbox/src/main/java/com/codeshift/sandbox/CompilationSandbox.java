package com.codeshift.sandbox;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Compiles generated Java sources and reports diagnostics — the deterministic
 * core of the Transformation Agent's compile-repair loop (product doc §5, agent
 * #4).
 *
 * <p>Phase 3 uses the in-process JDK compiler ({@code javax.tools}), which needs
 * no Docker and runs anywhere the platform runs. The production design isolates
 * untrusted client code in Docker/Firecracker; this class is the same contract
 * behind that boundary, so swapping in a container runner later is a drop-in.
 *
 * <p>Framework-agnostic (no Spring) so the execution core stays portable.
 */
public class CompilationSandbox {

    /** Compile all sources together; success means the whole set type-checks. */
    public CompileResult compile(List<JavaSourceFile> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompileResult(false, List.of("No system Java compiler available (JDK required)."));
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path outputDir;
        try {
            outputDir = Files.createTempDirectory("codeshift-sbx-");
        } catch (IOException e) {
            return new CompileResult(false, List.of("Could not create sandbox dir: " + e.getMessage()));
        }

        try (StandardJavaFileManager fm =
                compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

            List<JavaFileObject> units = new ArrayList<>();
            for (JavaSourceFile s : sources) {
                units.add(new InMemorySource(s.className(), s.source()));
            }

            boolean ok = compiler.getTask(null, fm, diagnostics, List.of(), null, units).call();
            List<String> messages = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .map(d -> formatDiagnostic(d))
                    .toList();
            return new CompileResult(ok && messages.isEmpty(), messages);
        } catch (IOException e) {
            return new CompileResult(false, List.of("Sandbox error: " + e.getMessage()));
        } finally {
            deleteRecursively(outputDir);
        }
    }

    private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> d) {
        String where = d.getSource() != null ? d.getSource().getName() + ":" + d.getLineNumber() : "?";
        return where + " " + d.getMessage(Locale.ROOT);
    }

    private static void deleteRecursively(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort
                }
            });
        } catch (IOException ignored) {
            // best-effort
        }
    }

    /** A source unit backed by an in-memory string. */
    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String code;

        InMemorySource(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
