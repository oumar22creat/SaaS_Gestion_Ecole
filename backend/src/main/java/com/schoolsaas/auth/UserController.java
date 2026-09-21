package com.schoolsaas.auth;

import com.schoolsaas.auth.dto.CreateUserRequest;
import com.schoolsaas.auth.dto.UpdateUserRequest;
import com.schoolsaas.auth.dto.UserResponse;
import com.schoolsaas.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.of(UserResponse.from(userService.getCurrentUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(UserResponse.from(userService.getById(id)));
    }

    /** Création d'un compte pour le personnel de l'établissement courant (§20.2). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.of(UserResponse.from(userService.create(request)));
    }

    /** Activation / désactivation. Un compte inactif est refusé à la connexion et au refresh. */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest.Status request) {
        return ApiResponse.of(UserResponse.from(userService.setActive(id, request.active())));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<UserResponse> updateRole(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest.RoleChange request) {
        return ApiResponse.of(UserResponse.from(userService.changeRole(id, request.role())));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<UserResponse> resetPassword(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest.PasswordReset request) {
        return ApiResponse.of(UserResponse.from(userService.resetPassword(id, request.password())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<List<UserResponse>> list(Pageable pageable) {
        Page<User> page = userService.list(pageable);
        List<UserResponse> data = page.map(UserResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }
}
