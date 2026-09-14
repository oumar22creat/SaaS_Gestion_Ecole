package com.schoolsaas.discipline;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.discipline.dto.ConvocationCreateRequest;
import com.schoolsaas.discipline.dto.ConvocationResponse;
import com.schoolsaas.discipline.dto.ConvocationStatusUpdateRequest;
import com.schoolsaas.discipline.dto.DisciplineStatisticsResponse;
import com.schoolsaas.discipline.dto.IncidentCreateRequest;
import com.schoolsaas.discipline.dto.IncidentResponse;
import com.schoolsaas.discipline.dto.ObservationCreateRequest;
import com.schoolsaas.discipline.dto.ObservationResponse;
import com.schoolsaas.discipline.dto.SanctionCreateRequest;
import com.schoolsaas.discipline.dto.SanctionResponse;
import com.schoolsaas.discipline.dto.StudentDisciplineHistoryResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Vie scolaire — cahier-des-charges.md §17, ROADMAP.md 3.1. */
@RestController
@RequestMapping("/api/v1/discipline")
public class DisciplineController {

    private final DisciplineService disciplineService;

    public DisciplineController(DisciplineService disciplineService) {
        this.disciplineService = disciplineService;
    }

    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
    public ApiResponse<IncidentResponse> createIncident(@Valid @RequestBody IncidentCreateRequest request) {
        Incident incident = disciplineService.createIncident(request, currentUserId());
        return ApiResponse.of(IncidentResponse.from(incident, disciplineService.studentIdsOfIncident(incident.getId())));
    }

    @GetMapping("/incidents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
    public ApiResponse<IncidentResponse> getIncident(@PathVariable Long id) {
        Incident incident = disciplineService.getIncident(id);
        return ApiResponse.of(IncidentResponse.from(incident, disciplineService.studentIdsOfIncident(id)));
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
    public ApiResponse<List<IncidentResponse>> listIncidents(
            @RequestParam Long schoolClassId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<IncidentResponse> data = disciplineService.listIncidentsForClass(schoolClassId, from, to).stream()
                .map(incident -> IncidentResponse.from(incident, disciplineService.studentIdsOfIncident(incident.getId())))
                .toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/incidents/{incidentId}/sanctions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'VIE_SCOLAIRE')")
    public ApiResponse<SanctionResponse> createSanction(@PathVariable Long incidentId, @Valid @RequestBody SanctionCreateRequest request) {
        Sanction sanction = disciplineService.createSanction(incidentId, request, currentUserId());
        return ApiResponse.of(SanctionResponse.from(sanction));
    }

    @GetMapping("/incidents/{incidentId}/sanctions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
    public ApiResponse<List<SanctionResponse>> listSanctions(@PathVariable Long incidentId) {
        List<SanctionResponse> data = disciplineService.listSanctionsForIncident(incidentId).stream().map(SanctionResponse::from).toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/convocations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'VIE_SCOLAIRE')")
    public ApiResponse<ConvocationResponse> createConvocation(@Valid @RequestBody ConvocationCreateRequest request) {
        Convocation convocation = disciplineService.createConvocation(request, currentUserId());
        return ApiResponse.of(ConvocationResponse.from(convocation));
    }

    @PutMapping("/convocations/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'VIE_SCOLAIRE')")
    public ApiResponse<ConvocationResponse> updateConvocationStatus(
            @PathVariable Long id, @Valid @RequestBody ConvocationStatusUpdateRequest request) {
        return ApiResponse.of(ConvocationResponse.from(disciplineService.updateConvocationStatus(id, request.status())));
    }

    @PostMapping("/observations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
    public ApiResponse<ObservationResponse> createObservation(@Valid @RequestBody ObservationCreateRequest request) {
        Observation observation = disciplineService.createObservation(request, currentUserId());
        return ApiResponse.of(ObservationResponse.from(observation));
    }

    @GetMapping("/students/{studentId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
    public ApiResponse<StudentDisciplineHistoryResponse> history(@PathVariable Long studentId) {
        return ApiResponse.of(disciplineService.historyForStudent(studentId));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'VIE_SCOLAIRE')")
    public ApiResponse<DisciplineStatisticsResponse> statistics(
            @RequestParam Long schoolClassId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.of(disciplineService.statisticsForClass(schoolClassId, from, to));
    }

    private Long currentUserId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).subjectId();
    }
}
