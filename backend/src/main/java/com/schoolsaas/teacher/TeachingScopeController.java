package com.schoolsaas.teacher;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.teacher.dto.TeachingScopeResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lecture seule du périmètre de l'utilisateur connecté (ADR-030). Sert uniquement à l'affichage :
 * chaque action reste contrôlée par {@link TeacherScope} dans son propre contrôleur. Aucun
 * paramètre — impossible d'interroger le périmètre d'un collègue.
 */
@RestController
public class TeachingScopeController {

    private final TeacherScope teacherScope;

    public TeachingScopeController(TeacherScope teacherScope) {
        this.teacherScope = teacherScope;
    }

    @GetMapping("/api/v1/me/teaching-scope")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT')")
    public ApiResponse<TeachingScopeResponse> current() {
        return ApiResponse.of(teacherScope.currentPerimeter()
                .map(TeachingScopeResponse::from)
                .orElseGet(TeachingScopeResponse::unrestricted));
    }
}
