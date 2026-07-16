package com.codeshift.datashift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Oracle-to-PostgreSQL DDL converter — the DataShift pillar.
 *
 * <p>Runs entirely in-process with no live database, so it works in the {@code nodb}
 * demo profile. Every substitution is recorded as a {@link DataShiftResult.TypeMapping}
 * for auditability, and constructs with no safe deterministic mapping are surfaced as
 * warnings rather than silently dropped. An LLM pass can refine the output later, but
 * the deterministic core is the source of truth.
 */
public class DdlConverter {

    /** Oracle data type -> PostgreSQL data type. Order matters: longer/prefix types first. */
    private static final Map<Pattern, String> TYPE_MAPPINGS = new LinkedHashMap<>();

    /** Oracle built-in function/keyword -> PostgreSQL equivalent. */
    private static final Map<Pattern, String> FUNCTION_MAPPINGS = new LinkedHashMap<>();

    /** Constructs we cannot safely convert — flagged as warnings for a human. */
    private static final Map<Pattern, String> UNSUPPORTED = new LinkedHashMap<>();

    static {
        // --- Type mappings (word-boundary, case-insensitive). ---
        typeMap("NUMBER\\s*\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\)", "NUMERIC"); // NUMBER(p,s)
        typeMap("NUMBER\\s*\\(\\s*\\d+\\s*\\)", "INTEGER");              // NUMBER(p)
        typeMap("NUMBER", "NUMERIC");
        typeMap("VARCHAR2", "VARCHAR");
        typeMap("NVARCHAR2", "VARCHAR");
        typeMap("NCHAR", "CHAR");
        typeMap("CLOB", "TEXT");
        typeMap("NCLOB", "TEXT");
        typeMap("LONG", "TEXT");
        typeMap("BLOB", "BYTEA");
        typeMap("RAW\\s*\\(\\s*\\d+\\s*\\)", "BYTEA");
        typeMap("RAW", "BYTEA");
        typeMap("BINARY_FLOAT", "REAL");
        typeMap("BINARY_DOUBLE", "DOUBLE PRECISION");
        typeMap("DATE", "TIMESTAMP");

        // --- Function / keyword mappings. ---
        funcMap("\\bSYSDATE\\b", "CURRENT_TIMESTAMP");
        funcMap("\\bSYSTIMESTAMP\\b", "CURRENT_TIMESTAMP");
        funcMap("\\bNVL\\s*\\(", "COALESCE(");
        funcMap("\\bSYS_GUID\\s*\\(\\s*\\)", "gen_random_uuid()");
        funcMap("\\bDUAL\\b", "");

        // --- Unsupported / needs-review constructs. ---
        unsupported("\\bCONNECT\\s+BY\\b", "CONNECT BY hierarchical query — rewrite as a recursive CTE (WITH RECURSIVE).");
        unsupported("\\bSTART\\s+WITH\\b", "START WITH — part of a CONNECT BY hierarchy; rewrite as a recursive CTE.");
        unsupported("\\(\\+\\)", "Oracle outer-join operator (+) — rewrite using ANSI LEFT/RIGHT JOIN.");
        unsupported("\\bCREATE\\s+SEQUENCE\\b", "Sequence — review; PostgreSQL supports SEQUENCE but options (NOCACHE, ORDER) differ.");
        unsupported("\\bCREATE\\s+OR\\s+REPLACE\\s+PACKAGE\\b", "PL/SQL package — no PostgreSQL equivalent; port to functions/schemas manually.");
        unsupported("\\bROWNUM\\b", "ROWNUM — rewrite using LIMIT or a window function (ROW_NUMBER()).");
    }

    private static void typeMap(String oracleRegex, String postgres) {
        TYPE_MAPPINGS.put(Pattern.compile("\\b" + oracleRegex, Pattern.CASE_INSENSITIVE), postgres);
    }

    private static void funcMap(String oracleRegex, String postgres) {
        FUNCTION_MAPPINGS.put(Pattern.compile(oracleRegex, Pattern.CASE_INSENSITIVE), postgres);
    }

    private static void unsupported(String oracleRegex, String warning) {
        UNSUPPORTED.put(Pattern.compile(oracleRegex, Pattern.CASE_INSENSITIVE), warning);
    }

    /** Convert Oracle DDL/SQL to PostgreSQL. Never throws on odd input — warnings capture gaps. */
    public DataShiftResult convert(String oracleDdl) {
        String ddl = oracleDdl == null ? "" : oracleDdl;
        List<DataShiftResult.TypeMapping> applied = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Flag unsupported constructs before rewriting so line context is intact.
        for (Map.Entry<Pattern, String> entry : UNSUPPORTED.entrySet()) {
            if (entry.getKey().matcher(ddl).find() && !warnings.contains(entry.getValue())) {
                warnings.add(entry.getValue());
            }
        }

        // Apply type mappings.
        for (Map.Entry<Pattern, String> entry : TYPE_MAPPINGS.entrySet()) {
            ddl = applyMapping(ddl, entry.getKey(), entry.getValue(), applied);
        }

        // Apply function/keyword mappings.
        for (Map.Entry<Pattern, String> entry : FUNCTION_MAPPINGS.entrySet()) {
            ddl = applyMapping(ddl, entry.getKey(), entry.getValue(), applied);
        }

        // Oracle statements terminated by "/" on its own line -> drop it (PostgreSQL uses ;).
        ddl = ddl.replaceAll("(?m)^\\s*/\\s*$", "");

        if (applied.isEmpty() && warnings.isEmpty()) {
            warnings.add("No Oracle-specific constructs detected — DDL may already be portable.");
        }

        return new DataShiftResult("Oracle", "PostgreSQL", ddl.strip(), applied, warnings);
    }

    private static String applyMapping(String ddl, Pattern pattern, String replacement,
                                       List<DataShiftResult.TypeMapping> applied) {
        Matcher matcher = pattern.matcher(ddl);
        int count = 0;
        String firstMatch = null;
        while (matcher.find()) {
            if (firstMatch == null) {
                firstMatch = matcher.group().trim();
            }
            count++;
        }
        if (count == 0) {
            return ddl;
        }
        applied.add(new DataShiftResult.TypeMapping(
                firstMatch, replacement.isEmpty() ? "(removed)" : replacement, count));
        return matcher.replaceAll(Matcher.quoteReplacement(replacement));
    }
}
