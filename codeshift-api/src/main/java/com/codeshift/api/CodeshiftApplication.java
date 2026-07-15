package com.codeshift.api;

import com.codeshift.gateway.ModelProfilesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * CodeShift control plane (Spring Boot).
 *
 * <p>Scans the whole {@code com.codeshift} tree so the gateway, graph and BSG
 * beans are picked up; Flyway (classpath {@code db/migration}) owns the schema.
 */
@SpringBootApplication(scanBasePackages = "com.codeshift")
@EntityScan(basePackages = "com.codeshift.bsg.entity")
@EnableJpaRepositories(basePackages = "com.codeshift.bsg.repo")
@EnableConfigurationProperties(ModelProfilesProperties.class)
public class CodeshiftApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeshiftApplication.class, args);
    }
}
