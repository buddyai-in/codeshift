package com.codeshift.api;

import com.codeshift.gateway.ModelProfilesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * CodeShift control plane (Spring Boot).
 *
 * <p>Scans the whole {@code com.codeshift} tree so the gateway, graph and
 * assessment beans are picked up. Persistence (JPA + BSG store) lives in
 * {@link PersistenceConfig}, which is disabled by the {@code nodb} profile so the
 * free-assessment funnel can run without a database.
 */
@SpringBootApplication(scanBasePackages = "com.codeshift")
@EnableConfigurationProperties(ModelProfilesProperties.class)
public class CodeshiftApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeshiftApplication.class, args);
    }
}
