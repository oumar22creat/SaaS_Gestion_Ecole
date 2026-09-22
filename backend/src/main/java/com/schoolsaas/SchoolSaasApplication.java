package com.schoolsaas;

import com.schoolsaas.common.TenantScopedRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// Redis ne sert qu'au compteur de limitation de débit, via StringRedisTemplate. Sans cette
// exclusion, Spring Data Redis tente de revendiquer les dépôts JPA du projet : il leur cède
// la main, mais l'ambiguïté est inutile et pollue les logs de démarrage.
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableJpaRepositories(repositoryBaseClass = TenantScopedRepositoryImpl.class)
@EnableScheduling
public class SchoolSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolSaasApplication.class, args);
    }
}
