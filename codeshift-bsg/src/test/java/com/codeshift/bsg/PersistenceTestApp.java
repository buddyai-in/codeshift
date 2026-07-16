package com.codeshift.bsg;

import com.codeshift.bsg.repo.BsgEdgeRepository;
import com.codeshift.bsg.repo.BsgNodeRepository;
import com.codeshift.bsg.repo.BsgVersionRepository;
import com.codeshift.bsg.repo.MigrationProjectRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Minimal Spring Boot app to exercise the JPA persistence layer against H2. */
@SpringBootApplication
public class PersistenceTestApp {

    @Bean
    BsgStore bsgStore(BsgVersionRepository v, BsgNodeRepository n, BsgEdgeRepository e) {
        return new BsgStore(v, n, e);
    }

    @Bean
    ProjectStore projectStore(MigrationProjectRepository p) {
        return new ProjectStore(p);
    }
}
