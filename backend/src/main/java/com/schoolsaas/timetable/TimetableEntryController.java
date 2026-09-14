package com.schoolsaas.timetable;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.timetable.dto.TimetableEntryRequest;
import com.schoolsaas.timetable.dto.TimetableEntryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * CRUD emploi du temps + détection de conflits — cahier-des-charges.md §9, ROADMAP.md 1.6.
 * Vues par classe/enseignant (cahier §9) via les paramètres {@code schoolClassId}/
 * {@code teacherId} plutôt que des endpoints dédiés — la vue journalière/hebdomadaire reste
 * un simple filtrage côté client des créneaux renvoyés.
 */
@RestController
@RequestMapping("/api/v1/timetable-entries")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class TimetableEntryController {

    private final TimetableEntryService timetableEntryService;

    public TimetableEntryController(TimetableEntryService timetableEntryService) {
        this.timetableEntryService = timetableEntryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TimetableEntryResponse> create(@Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.of(TimetableEntryResponse.from(timetableEntryService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TimetableEntryResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(TimetableEntryResponse.from(timetableEntryService.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<TimetableEntryResponse>> list(
            @RequestParam(required = false) Long schoolClassId, @RequestParam(required = false) Long teacherId) {
        List<TimetableEntry> entries;
        if (schoolClassId != null) {
            entries = timetableEntryService.listForClass(schoolClassId);
        } else if (teacherId != null) {
            entries = timetableEntryService.listForTeacher(teacherId);
        } else {
            entries = timetableEntryService.listAll();
        }
        return ApiResponse.of(entries.stream().map(TimetableEntryResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ApiResponse<TimetableEntryResponse> update(@PathVariable Long id, @Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.of(TimetableEntryResponse.from(timetableEntryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        timetableEntryService.delete(id);
    }
}
