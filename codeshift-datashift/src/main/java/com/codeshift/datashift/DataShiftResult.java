package com.codeshift.datashift;

import java.io.Serializable;
import java.util.List;

/**
 * Outcome of a DataShift DDL conversion.
 *
 * @param sourceDialect the detected/assumed source dialect (e.g. "Oracle")
 * @param targetDialect  the target dialect (e.g. "PostgreSQL")
 * @param convertedDdl   the rewritten DDL, ready to run on the target
 * @param mappings       every type/function substitution applied, for auditability
 * @param warnings       constructs that need a human eye (no deterministic mapping)
 */
public record DataShiftResult(
        String sourceDialect,
        String targetDialect,
        String convertedDdl,
        List<TypeMapping> mappings,
        List<String> warnings) implements Serializable {

    /** A single source-to-target substitution the converter applied. */
    public record TypeMapping(String source, String target, int occurrences) implements Serializable {
    }
}
