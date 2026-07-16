package com.codeshift.api;

import com.codeshift.datashift.DataShiftResult;
import com.codeshift.datashift.DdlConverter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * DataShift endpoint: deterministic Oracle-to-PostgreSQL DDL conversion.
 * Needs no live database, so it works in every profile (including {@code nodb}).
 */
@RestController
public class DataShiftController {

    private final DdlConverter converter = new DdlConverter();

    public record ConvertRequest(String ddl) {
    }

    @PostMapping("/datashift/convert")
    public DataShiftResult convert(@RequestBody ConvertRequest request) {
        return converter.convert(request.ddl());
    }
}
