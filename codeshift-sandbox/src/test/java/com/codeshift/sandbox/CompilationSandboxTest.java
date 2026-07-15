package com.codeshift.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CompilationSandboxTest {

    private final CompilationSandbox sandbox = new CompilationSandbox();

    @Test
    void compilesValidSource() {
        CompileResult r = sandbox.compile(List.of(new JavaSourceFile(
                "com.acme.Order",
                "package com.acme; public class Order { public int total() { return 42; } }")));
        assertThat(r.success()).isTrue();
        assertThat(r.diagnostics()).isEmpty();
    }

    @Test
    void reportsDiagnosticsForInvalidSource() {
        CompileResult r = sandbox.compile(List.of(new JavaSourceFile(
                "com.acme.Broken",
                "package com.acme; public class Broken { public int total() { return \"nope\"; } }")));
        assertThat(r.success()).isFalse();
        assertThat(r.diagnostics()).isNotEmpty();
    }

    @Test
    void compilesMultipleInterdependentSources() {
        CompileResult r = sandbox.compile(List.of(
                new JavaSourceFile("com.acme.Repo", "package com.acme; public class Repo { public String load() { return \"x\"; } }"),
                new JavaSourceFile("com.acme.Svc", "package com.acme; public class Svc { private final Repo r = new Repo(); public String go() { return r.load(); } }")));
        assertThat(r.success()).isTrue();
    }
}
