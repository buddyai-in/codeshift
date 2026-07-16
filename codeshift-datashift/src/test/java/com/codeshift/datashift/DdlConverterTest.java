package com.codeshift.datashift;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DdlConverterTest {

    private final DdlConverter converter = new DdlConverter();

    @Test
    void mapsOracleTypesToPostgres() {
        String oracle = """
                CREATE TABLE customer (
                  id        NUMBER(10),
                  balance   NUMBER(12,2),
                  name      VARCHAR2(100),
                  notes     CLOB,
                  avatar    BLOB,
                  created   DATE
                );
                """;

        DataShiftResult result = converter.convert(oracle);

        assertThat(result.targetDialect()).isEqualTo("PostgreSQL");
        assertThat(result.convertedDdl())
                .contains("id        INTEGER")
                .contains("balance   NUMERIC")
                .contains("name      VARCHAR(100)")
                .contains("notes     TEXT")
                .contains("avatar    BYTEA")
                .contains("created   TIMESTAMP")
                .doesNotContain("VARCHAR2")
                .doesNotContain("NUMBER")
                .doesNotContain("CLOB");
    }

    @Test
    void mapsFunctionsAndKeywords() {
        String oracle = "SELECT NVL(name, 'n/a') AS n, SYSDATE FROM DUAL;";

        DataShiftResult result = converter.convert(oracle);

        assertThat(result.convertedDdl())
                .contains("COALESCE(name")
                .contains("CURRENT_TIMESTAMP")
                .doesNotContain("SYSDATE")
                .doesNotContain("NVL(");
    }

    @Test
    void recordsEveryMappingForAudit() {
        DataShiftResult result = converter.convert("id NUMBER(10), name VARCHAR2(50)");

        assertThat(result.mappings()).isNotEmpty();
        assertThat(result.mappings())
                .anySatisfy(m -> assertThat(m.target()).isEqualTo("VARCHAR"));
    }

    @Test
    void flagsUnsupportedConstructsAsWarnings() {
        String oracle = """
                SELECT e.name, m.name
                FROM emp e, emp m
                WHERE e.mgr = m.id (+)
                CONNECT BY PRIOR e.id = e.mgr;
                """;

        DataShiftResult result = converter.convert(oracle);

        assertThat(result.warnings())
                .anySatisfy(w -> assertThat(w).contains("outer-join"))
                .anySatisfy(w -> assertThat(w).contains("CONNECT BY"));
    }

    @Test
    void warnsWhenNothingToConvert() {
        DataShiftResult result = converter.convert("CREATE TABLE t (id INTEGER);");

        assertThat(result.warnings())
                .anySatisfy(w -> assertThat(w).contains("already be portable"));
    }
}
