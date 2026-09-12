package com.schoolsaas.auth;

import com.schoolsaas.auth.dto.LoginRequest;
import com.schoolsaas.auth.dto.RefreshRequest;
import com.schoolsaas.auth.dto.TokenPairResponse;
import com.schoolsaas.common.ApiResponse;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(toResponse(authService.login(request.email(), request.password())));
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
