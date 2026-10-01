package com.schoolsaas.auth;

import com.schoolsaas.common.RestSecurityHandlers;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestSecurityHandlers restSecurityHandlers;
    private final CorsProperties corsProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestSecurityHandlers restSecurityHandlers,
            CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restSecurityHandlers = restSecurityHandlers;
        this.corsProperties = corsProperties;
    }

    /**
     * Origines fixes du WebView de l'application mobile (voir corsConfigurationSource).
     * {@code http://localhost} est l'origine de la compilation de vérification sur émulateur,
     * qui bascule en clair pour joindre un backend de développement.
     */
    private static final List<String> ORIGINES_APPLICATION_MOBILE =
            List.of("https://localhost", "capacitor://localhost", "ionic://localhost", "http://localhost");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Origines autorisées à appeler l'API depuis un navigateur.
     *
     * <p>{@code setAllowedOriginPatterns} et non {@code setAllowedOrigins} : chaque
     * établissement vit sur son propre sous-domaine, et une liste d'origines exactes ne peut
     * pas les couvrir. Les motifs acceptent aussi les valeurs exactes, les origines de
     * développement restent donc valables telles quelles.
     *
     * <p>Le CORS s'applique en production, contrairement à ce qui était supposé ici : les
     * navigateurs envoient un en-tête {@code Origin} sur les POST même lorsque la page et
     * l'API partagent le domaine, et Spring traite toute requête portant cet en-tête comme
     * une requête CORS. Avec la seule origine du domaine nu, chaque école recevait
     * « Invalid CORS request » en 403 sur la moindre écriture.
     *
     * <p>Les origines de l'application mobile s'ajoutent d'office à celles configurées. Un
     * WebView Capacitor ne sert pas l'application depuis le domaine de l'API mais depuis une
     * origine locale fixe — {@code https://localhost} sur Android, {@code capacitor://localhost}
     * sur iOS — et ses appels sont donc soumis au CORS comme ceux d'un navigateur. Ces valeurs
     * ne dépendent d'aucun déploiement : les laisser à la charge de la configuration revenait
     * à livrer une application mobile dont chaque requête échouait en 403, l'écran de
     * connexion compris.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origines = new ArrayList<>(corsProperties.allowedOrigins());
        for (String origineMobile : ORIGINES_APPLICATION_MOBILE) {
            if (!origines.contains(origineMobile)) {
                origines.add(origineMobile);
            }
        }
        configuration.setAllowedOriginPatterns(origines);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/admin/auth/**",
                                "/api/v1/tenants/register",
                                "/api/v1/billing/plans",
                                "/api/v1/billing/webhooks/**",
                                // Appelé par l'opérateur mobile money, qui n'a pas de compte
                                // chez nous : protégé par un secret partagé, pas par un jeton.
                                "/api/v1/payments/mobile-money/callback",
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        // Appelé par Web/Mobile au démarrage, avant toute connexion, pour
                        // appliquer le branding du tenant résolu par sous-domaine/en-tête (voir
                        // TenantResolver/TenantSettingsController, ROADMAP.md 3.7/ADR-028).
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/tenants/current/branding",
                                // Le logo accompagne le branding : il s'affiche sur l'écran de
                                // connexion, donc avant toute authentification.
                                "/api/v1/tenants/current/logo")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restSecurityHandlers)
                        .accessDeniedHandler(restSecurityHandlers))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
