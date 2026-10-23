package com.schoolsaas.timetable;

import com.schoolsaas.common.PagedList;
import com.schoolsaas.teacher.TeacherScope;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.timetable.dto.TimetableEntryRequest;
import com.schoolsaas.timetable.dto.TimetableEntryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
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
 *
 * <p>Un enseignant ne consulte que SON emploi du temps ({@link TeacherScope}, ADR-030) : sans
 * filtre, il reçoit ses créneaux ; un {@code schoolClassId} restreint ses créneaux à cette
 * classe ; un {@code teacherId} qui n'est pas le sien est refusé en 403.
 */
@RestController
@RequestMapping("/api/v1/timetable-entries")
public class TimetableEntryController {

    private final TimetableEntryService timetableEntryService;
    private final TeacherScope teacherScope;

    public TimetableEntryController(TimetableEntryService timetableEntryService, TeacherScope teacherScope) {
        this.timetableEntryService = timetableEntryService;
        this.teacherScope = teacherScope;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<TimetableEntryResponse> create(@Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.of(TimetableEntryResponse.from(timetableEntryService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
    public ApiResponse<TimetableEntryResponse> getById(@PathVariable Long id) {
        TimetableEntry entry = timetableEntryService.getById(id);
        teacherScope.requireOwnTeacher(entry.getTeacherId());
        return ApiResponse.of(TimetableEntryResponse.from(entry));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
    public ApiResponse<List<TimetableEntryResponse>> list(
            Pageable pageable,
            @RequestParam(required = false) Long schoolClassId,
            @RequestParam(required = false) Long teacherId) {
        List<TimetableEntry> entries;
        var perimeter = teacherScope.currentPerimeter();
        if (perimeter.isPresent()) {
            if (teacherId != null) {
                teacherScope.requireOwnTeacher(teacherId);
            }
            entries = timetableEntryService.listForTeachers(perimeter.get().teacherIds()).stream()
                    .filter(entry -> teacherId == null || entry.getTeacherId().equals(teacherId))
                    .filter(entry -> schoolClassId == null || entry.getSchoolClassId().equals(schoolClassId))
                    .toList();
        } else if (schoolClassId != null) {
            entries = timetableEntryService.listForClass(schoolClassId);
        } else if (teacherId != null) {
            entries = timetableEntryService.listForTeacher(teacherId);
        } else {
            entries = timetableEntryService.listAll();
        }
        // Filtré par classe ou par enseignant, le résultat tient dans une poignée de lignes.
        // Sans filtre, c'est toute la grille de l'établissement : trente classes font deux
        // cent quarante créneaux, qu'aucun écran n'affiche d'un bloc.
        return PagedList.response(
                PagedList.of(entries.stream().map(TimetableEntryResponse::from).toList(), pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<TimetableEntryResponse> update(@PathVariable Long id, @Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.of(TimetableEntryResponse.from(timetableEntryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public void delete(@PathVariable Long id) {
        timetableEntryService.delete(id);
    }
}
