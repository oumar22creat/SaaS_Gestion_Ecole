package com.schoolsaas.tenant;

import com.schoolsaas.billing.PlanFeatureInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantWebConfig implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;
    private final TenantAccessInterceptor tenantAccessInterceptor;
    private final PlanFeatureInterceptor planFeatureInterceptor;

    public TenantWebConfig(
            TenantContextInterceptor tenantContextInterceptor,
            TenantAccessInterceptor tenantAccessInterceptor,
            PlanFeatureInterceptor planFeatureInterceptor) {
        this.tenantContextInterceptor = tenantContextInterceptor;
        this.tenantAccessInterceptor = tenantAccessInterceptor;
        this.planFeatureInterceptor = planFeatureInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // order élevé : doit s'exécuter APRÈS OpenEntityManagerInViewInterceptor (enregistré
        // par Spring Boot pour la même chaîne d'intercepteurs MVC), sans quoi l'EntityManager
        // partagé n'est pas encore lié au thread de la requête et enableFilter() n'a alors
        // aucun effet durable (bug mis en évidence par TenantIsolationTest).
        registry.addInterceptor(tenantContextInterceptor).addPathPatterns("/api/**").order(100);
        // Doit s'exécuter APRÈS TenantContextInterceptor : dépend du tenant déjà résolu dans
        // TenantContext (voir TenantAccessInterceptor).
        registry.addInterceptor(tenantAccessInterceptor).addPathPatterns("/api/**").order(101);
        // Doit aussi s'exécuter après TenantContextInterceptor, pour la même raison — voir
        // PlanFeatureInterceptor (ROADMAP.md 3.7).
        registry.addInterceptor(planFeatureInterceptor).addPathPatterns("/api/**").order(102);
    }
}
