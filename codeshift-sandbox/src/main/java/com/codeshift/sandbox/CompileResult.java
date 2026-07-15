package com.codeshift.sandbox;

import java.io.Serializable;
import java.util.List;

/** Outcome of a sandbox compilation: success flag + compiler diagnostics. */
public record CompileResult(boolean success, List<String> diagnostics) implements Serializable {

    public static CompileResult ok() {
        return new CompileResult(true, List.of());
    }
}
