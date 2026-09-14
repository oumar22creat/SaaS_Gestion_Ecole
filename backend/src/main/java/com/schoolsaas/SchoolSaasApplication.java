package com.schoolsaas;

import com.schoolsaas.common.TenantScopedRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaRepositories(repositoryBaseClass = TenantScopedRepositoryImpl.class)
@EnableScheduling
public class SchoolSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolSaasApplication.class, args);
    }
}
