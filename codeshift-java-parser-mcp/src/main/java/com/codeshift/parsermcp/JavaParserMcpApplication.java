package com.codeshift.parsermcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Java parser capability server (MCP, stdio).
 *
 * <p>Exposes deterministic JavaParser analysis — module inventory, dependency
 * graph, messaging detection, and a free assessment — as MCP tools. This is the
 * "any agent, any language can call the parser" plugin from the design: the
 * Discovery/Analysis agents (or a cross-language orchestrator) call these tools
 * instead of embedding a parser. No database required.
 */
@SpringBootApplication
public class JavaParserMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaParserMcpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider javaParserTools(JavaParserToolService service) {
        return MethodToolCallbackProvider.builder().toolObjects(service).build();
    }
}
