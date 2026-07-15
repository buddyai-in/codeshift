package com.codeshift.parsermcp;

import com.codeshift.assessment.AssessmentGenerator;
import com.codeshift.assessment.AssessmentReport;
import com.codeshift.parser.JavaProjectAnalyzer;
import com.codeshift.parser.ProjectAnalysis;
import java.nio.file.Path;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** JavaParser analysis exposed as MCP tools. */
@Service
public class JavaParserToolService {

    @Tool(description = "Analyze a Java source directory: module inventory, dependency graph, "
            + "messaging systems, and pre-Jakarta (javax.*) signal.")
    public ProjectAnalysis analyzeJavaProject(
            @ToolParam(description = "Absolute path to the Java source root") String projectPath) {
        return JavaProjectAnalyzer.analyze(Path.of(projectPath));
    }

    @Tool(description = "Return the leaf-first translation order (dependencies before dependents) "
            + "for a Java source directory.")
    public List<String> translationOrder(
            @ToolParam(description = "Absolute path to the Java source root") String projectPath) {
        return JavaProjectAnalyzer.analyze(Path.of(projectPath)).translationOrder();
    }

    @Tool(description = "Produce a free migration assessment (effort, price estimate, migration "
            + "signals) for a Java source directory.")
    public AssessmentReport assessJavaProject(
            @ToolParam(description = "Absolute path to the Java source root") String projectPath,
            @ToolParam(description = "Human-readable project name") String projectName) {
        ProjectAnalysis analysis = JavaProjectAnalyzer.analyze(Path.of(projectPath));
        return AssessmentGenerator.generate(projectName, analysis);
    }
}
