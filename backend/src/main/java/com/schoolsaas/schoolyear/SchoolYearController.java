package com.schoolsaas.schoolyear;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.schoolyear.dto.EnrollmentResponse;
import com.schoolsaas.schoolyear.dto.PromotionRequest;
import com.schoolsaas.schoolyear.dto.PromotionResult;
import com.schoolsaas.schoolyear.dto.SchoolYearRequest;
import com.schoolsaas.schoolyear.dto.SchoolYearResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Années scolaires et rentrée (cahier-des-charges.md §7).
 *
 * <p>La lecture est ouverte à tout le personnel : l'année courante est le cadre de tous les
 * écrans. Créer, activer ou clôturer une année engage l'établissement entier et reste à la
 * direction.
 */
@RestController
@RequestMapping("/api/v1/school-years")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class SchoolYearController {

    private final SchoolYearService schoolYearService;

    public SchoolYearController(SchoolYearService schoolYearService) {
        this.schoolYearService = schoolYearService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT')")
    public ApiResponse<List<SchoolYearResponse>> list() {
        return ApiResponse.of(schoolYearService.list().stream()
                .map(year -> SchoolYearResponse.from(year, schoolYearService.enrolledCount(year.getId())))
                .toList());
    }

    /** L'année sur laquelle travaille le produit. 204 tant qu'aucune n'est activée. */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT')")
    public ApiResponse<SchoolYearResponse> active() {
        return ApiResponse.of(schoolYearService.active()
                .map(year -> SchoolYearResponse.from(year, schoolYearService.enrolledCount(year.getId())))
                .orElse(null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SchoolYearResponse> create(@Valid @RequestBody SchoolYearRequest request) {
        SchoolYear year = schoolYearService.create(request);
        return ApiResponse.of(SchoolYearResponse.from(year, 0));
    }

    @PutMapping("/{id}/activate")
    public ApiResponse<SchoolYearResponse> activate(@PathVariable Long id) {
        SchoolYear year = schoolYearService.activate(id);
        return ApiResponse.of(SchoolYearResponse.from(year, schoolYearService.enrolledCount(id)));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<SchoolYearResponse> close(@PathVariable Long id) {
        SchoolYear year = schoolYearService.close(id);
        return ApiResponse.of(SchoolYearResponse.from(year, schoolYearService.enrolledCount(id)));
    }

    /** Réinscrit une promotion entière sur l'année cible : c'est l'opération de rentrée. */
    @PostMapping("/{id}/promotions")
    public ApiResponse<PromotionResult> promote(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.of(schoolYearService.promote(id, request));
    }

    /** Parcours d'un élève : dans quelle classe il était, année par année. */
    @GetMapping("/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<List<EnrollmentResponse>> historyForStudent(@RequestParam Long studentId) {
        return ApiResponse.of(schoolYearService.historyForStudent(studentId));
    }
}
