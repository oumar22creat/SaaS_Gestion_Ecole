package com.schoolsaas.tenant;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantWebConfig implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;

    public TenantWebConfig(TenantContextInterceptor tenantContextInterceptor) {
        this.tenantContextInterceptor = tenantContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // order élevé : doit s'exécuter APRÈS OpenEntityManagerInViewInterceptor (enregistré
        // par Spring Boot pour la même chaîne d'intercepteurs MVC), sans quoi l'EntityManager
        // partagé n'est pas encore lié au thread de la requête et enableFilter() n'a alors
        // aucun effet durable (bug mis en évidence par TenantIsolationTest).
        registry.addInterceptor(tenantContextInterceptor).addPathPatterns("/api/**").order(100);
    }
}
