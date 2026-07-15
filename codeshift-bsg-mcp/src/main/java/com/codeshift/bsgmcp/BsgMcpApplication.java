package com.codeshift.bsgmcp;

import com.codeshift.bsg.BsgStore;
import com.codeshift.bsg.repo.BsgRepositories.BsgEdgeRepository;
import com.codeshift.bsg.repo.BsgRepositories.BsgNodeRepository;
import com.codeshift.bsg.repo.BsgRepositories.BsgVersionRepository;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * BSG capability server (MCP, stdio transport).
 *
 * <p>Exposes the BSG store as typed MCP tools so any agent — or another service —
 * touches the BSG through one uniform contract instead of ad-hoc SQL. This is the
 * pattern for every capability in the design: parsers, sandbox, git, security each
 * become an MCP server just like this one.
 *
 * <p>Run against a live Postgres: {@code java -jar codeshift-bsg-mcp.jar}.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.codeshift.bsg.entity")
@EnableJpaRepositories(basePackages = "com.codeshift.bsg.repo")
public class BsgMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(BsgMcpApplication.class, args);
    }

    /** Register the BSG tool methods as MCP tools. */
    @Bean
    ToolCallbackProvider bsgTools(BsgToolService bsgToolService) {
        return MethodToolCallbackProvider.builder().toolObjects(bsgToolService).build();
    }

    @Bean
    BsgToolService bsgToolService(BsgStore store) {
        return new BsgToolService(store);
    }

    /** BsgStore is a plain class (not component-scanned) — declare it as a bean. */
    @Bean
    BsgStore bsgStore(BsgVersionRepository versions, BsgNodeRepository nodes,
            BsgEdgeRepository edges) {
        return new BsgStore(versions, nodes, edges);
    }
}
