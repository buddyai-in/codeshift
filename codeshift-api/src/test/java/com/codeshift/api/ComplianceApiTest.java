package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeshift.bsg.BsgStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Web-slice test for the compliance endpoints — standalone MockMvc, no database.
 * Proves the inline check works in any profile: seed a project with the PCI
 * template and it reports 100% compliant; a generic BSG reports gaps.
 */
class ComplianceApiTest {

    private static <T> ObjectProvider<T> none() {
        return new ObjectProvider<>() {
            @Override public T getObject() { throw new UnsupportedOperationException(); }
            @Override public T getObject(Object... args) { throw new UnsupportedOperationException(); }
            @Override public T getIfAvailable() { return null; }
            @Override public T getIfUnique() { return null; }
        };
    }

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ComplianceController(ComplianceApiTest.<BsgStore>none()))
            .build();

    @Test
    void listsStandards() throws Exception {
        mvc.perform(get("/compliance/standards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.standard=='PCI_DSS')].reference").value("PCI-DSS v4.0"))
                .andExpect(jsonPath("$[?(@.standard=='HIPAA')]").isNotEmpty());
    }

    @Test
    void inlineCheckOfSeededTemplateIsFullyCompliant() throws Exception {
        // Pull the PCI template, then check it against PCI — expect 100%.
        String template = mvc.perform(get("/compliance/standards/PCI_DSS/template"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String body = "{\"standard\":\"PCI_DSS\",\"bsg\":" + template + "}";
        mvc.perform(post("/compliance/check")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.score").value(100));
    }

    @Test
    void inlineCheckOfGenericBsgReportsGaps() throws Exception {
        String body = """
                {"standard":"HIPAA","bsg":{"projectId":"p","versionNumber":1,"edges":[],
                 "nodes":[{"nodeRef":"BSG-001","nodeType":"BUSINESS_RULE","title":"Order total",
                   "description":"Sums line items","sourceLocation":"OrderService",
                   "confidence":"HIGH","humanStatus":"PENDING","origin":"MIGRATED",
                   "targetCodeLocation":null,"testCoverage":false}]}}
                """;
        mvc.perform(post("/compliance/check")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.coveredControls").value(0))
                .andExpect(jsonPath("$.results[0].remediation").isNotEmpty());
    }
}
