package com.schoolsaas.auth;

import com.schoolsaas.auth.dto.RefreshRequest;
import com.schoolsaas.auth.dto.TenantLoginRequest;
import com.schoolsaas.auth.dto.TokenPairResponse;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.common.ratelimit.ClientIpResolver;
import com.schoolsaas.common.ratelimit.RateLimitProperties;
import com.schoolsaas.common.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connexion/rafraîchissement/déconnexion pour les comptes d'un établissement. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public AuthController(
            AuthService authService, RateLimiter rateLimiter, RateLimitProperties rateLimitProperties) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    /**
     * Deux compteurs plutôt qu'un : celui par IP freine un balayage de comptes depuis une
     * même machine, celui par compte protège un utilisateur précis même si l'attaque vient
     * de centaines d'adresses différentes. Les deux sont consommés avant toute vérification
     * du mot de passe, pour qu'une tentative coûte quelque chose même quand elle échoue.
     */
    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(
            @Valid @RequestBody TenantLoginRequest request, HttpServletRequest httpRequest) {
        rateLimiter.consume("login-ip", ClientIpResolver.resolve(httpRequest), rateLimitProperties.loginPerIp());
        rateLimiter.consume(
                "login-account",
                request.subdomain() + "|" + request.email(),
                rateLimitProperties.loginPerAccount());
        return ApiResponse.of(toResponse(authService.login(request.subdomain(), request.email(), request.password())));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of(toResponse(authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    private TokenPairResponse toResponse(AuthService.TokenPair pair) {
        return new TokenPairResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }
}
