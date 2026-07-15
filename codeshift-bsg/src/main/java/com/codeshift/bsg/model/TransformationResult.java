package com.codeshift.bsg.model;

import java.io.Serializable;
import java.util.List;

/**
 * Output of the Transformation + Test Generation agents (product doc §5, agents
 * #4/#5): the generated target code and tests, plus whether it all compiled in
 * the sandbox.
 */
public record TransformationResult(
        List<TransformedModule> modules,
        List<GeneratedTest> tests,
        boolean allCompiled,
        List<String> diagnostics) implements Serializable {

    /** One migrated module: target class + generated source + compile status. */
    public record TransformedModule(
            String moduleId,
            String targetClass,
            String layer,
            String sourceCode,
            boolean compiled,
            List<String> bsgRuleRefs) implements Serializable {}

    /** One generated JUnit 5 test, traceable to a BSG rule. */
    public record GeneratedTest(String testClass, String sourceCode, String bsgRuleRef)
            implements Serializable {}
}
