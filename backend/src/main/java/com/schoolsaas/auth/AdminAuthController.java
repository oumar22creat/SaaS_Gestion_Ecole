package com.schoolsaas.auth;

import com.schoolsaas.auth.dto.LoginRequest;
import com.schoolsaas.auth.dto.TokenPairResponse;
import com.schoolsaas.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connexion Super-Administrateur (voir docs/API_CONVENTIONS.md — namespace /api/v1/admin). */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.TokenPair pair = authService.adminLogin(request.email(), request.password());
        return ApiResponse.of(new TokenPairResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds()));
    }
}
