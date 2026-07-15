package com.codeshift.parser;

import java.util.List;

/**
 * One legacy source unit (a top-level Java type). The Discovery Agent's atom:
 * the thing that gets a translation order, a transformation task, and BSG nodes.
 */
public record SourceModule(
        String id, // fully-qualified name, e.g. com.acme.OrderService
        String simpleName,
        String packageName,
        String filePath,
        int linesOfCode,
        boolean entryPoint,      // has main(), or a web entry annotation
        List<String> imports) {}
