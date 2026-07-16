package com.codeshift.api;

import com.codeshift.bsg.BsgStore;
import com.codeshift.bsg.MeteringStore;
import com.codeshift.bsg.PaymentStore;
import com.codeshift.bsg.ProjectStore;
import com.codeshift.bsg.SecretCipher;
import com.codeshift.bsg.TenantSecretStore;
import com.codeshift.bsg.TenantStore;
import com.codeshift.bsg.repo.BsgEdgeRepository;
import com.codeshift.bsg.repo.BsgNodeRepository;
import com.codeshift.bsg.repo.BsgVersionRepository;
import com.codeshift.bsg.repo.MigrationProjectRepository;
import com.codeshift.bsg.repo.OrganizationRepository;
import com.codeshift.bsg.repo.PaymentRepository;
import com.codeshift.bsg.repo.TenantSecretRepository;
import com.codeshift.bsg.repo.UsageEventRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Persistence wiring — active unless the {@code nodb} profile is set.
 *
 * <p>The free assessment funnel touches no database, so running with
 * {@code --spring.profiles.active=nodb} boots the app with zero infrastructure
 * (JPA/Flyway/DataSource auto-config is excluded for that profile in
 * {@code application.yml}). The full platform (runs, BSG) uses the default profile.
 */
@Configuration
@Profile("!nodb")
@EntityScan(basePackages = "com.codeshift.bsg.entity")
@EnableJpaRepositories(basePackages = "com.codeshift.bsg.repo")
public class PersistenceConfig {

    @Bean
    BsgStore bsgStore(BsgVersionRepository versions, BsgNodeRepository nodes,
            BsgEdgeRepository edges) {
        return new BsgStore(versions, nodes, edges);
    }

    @Bean
    ProjectStore projectStore(MigrationProjectRepository projects) {
        return new ProjectStore(projects);
    }

    @Bean
    TenantStore tenantStore(OrganizationRepository orgs) {
        return new TenantStore(orgs);
    }

    @Bean
    MeteringStore meteringStore(UsageEventRepository usage, ProjectStore projects) {
        return new MeteringStore(usage, projects);
    }

    @Bean
    PaymentStore paymentStore(PaymentRepository payments) {
        return new PaymentStore(payments);
    }

    @Bean
    TenantSecretStore tenantSecretStore(TenantSecretRepository secrets, SecretCipher cipher) {
        return new TenantSecretStore(secrets, cipher);
    }
}
