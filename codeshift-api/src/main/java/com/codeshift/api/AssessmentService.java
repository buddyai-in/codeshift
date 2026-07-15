package com.codeshift.api;

import com.codeshift.assessment.AssessmentGenerator;
import com.codeshift.assessment.AssessmentReport;
import com.codeshift.parser.JavaProjectAnalyzer;
import com.codeshift.parser.ProjectAnalysis;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * The free assessment use case: parse a codebase → dependency graph → report.
 * No account, no DB, no LLM — the product's top-of-funnel lead magnet.
 */
@Service
public class AssessmentService {

    /** Assess a server-accessible Java source directory. */
    public AssessmentReport assessDirectory(String projectName, Path projectRoot) {
        ProjectAnalysis analysis = JavaProjectAnalyzer.analyze(projectRoot);
        return AssessmentGenerator.generate(projectName, analysis);
    }

    /** Assess an uploaded source zip (extracted to a temp dir, then cleaned up). */
    public AssessmentReport assessZip(String projectName, InputStream zipStream) throws IOException {
        Path dir = null;
        try {
            dir = ZipExtractor.extractToTempDir(zipStream);
            return assessDirectory(projectName, dir);
        } finally {
            ZipExtractor.deleteRecursively(dir);
        }
    }
}
