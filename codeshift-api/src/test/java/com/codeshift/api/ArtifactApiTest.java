package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Web-slice test for artifact storage — standalone MockMvc, no database (object
 * storage works in every profile). Proves the put/get/list/delete round-trip.
 */
class ArtifactApiTest {

    private MockMvc mvc(Path dir) {
        return MockMvcBuilders
                .standaloneSetup(new ArtifactController(new LocalArtifactStore(dir.toString())))
                .build();
    }

    @Test
    void putGetListDelete(@TempDir Path dir) throws Exception {
        MockMvc mvc = mvc(dir);
        byte[] body = "hello codeshift".getBytes();

        mvc.perform(put("/artifacts/greeting.txt")
                        .contentType(MediaType.TEXT_PLAIN).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("greeting.txt"))
                .andExpect(jsonPath("$.size").value(body.length));

        mvc.perform(get("/artifacts/greeting.txt"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(body));

        mvc.perform(get("/artifacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("greeting.txt"));

        mvc.perform(delete("/artifacts/greeting.txt")).andExpect(status().isOk());
        mvc.perform(get("/artifacts/greeting.txt")).andExpect(status().isNotFound());
    }

    @Test
    void rejectsBadKey(@TempDir Path dir) throws Exception {
        mvc(dir).perform(put("/artifacts/..")
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isBadRequest());
    }
}
