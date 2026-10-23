package com.schoolsaas.homework;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.teacher.TeacherScope;
import com.schoolsaas.homework.dto.LessonRequest;
import com.schoolsaas.homework.dto.LessonResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Cahier de textes et devoirs — cahier-des-charges.md §13, ROADMAP.md 2.3.
 *
 * <p>Périmètre enseignant ({@link TeacherScope}, ADR-030) : il LIT le cahier de textes de ses
 * classes (toutes matières, pour équilibrer la charge de devoirs), mais n'ÉCRIT que pour les
 * couples (classe, matière) qu'il enseigne.
 */
@RestController
@RequestMapping("/api/v1/lessons")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
public class LessonController {

    private final LessonService lessonService;
    private final TeacherScope teacherScope;

    public LessonController(LessonService lessonService, TeacherScope teacherScope) {
        this.lessonService = lessonService;
        this.teacherScope = teacherScope;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonResponse> create(@Valid @RequestBody LessonRequest request) {
        teacherScope.requireClassSubject(request.schoolClassId(), request.subjectId());
        return ApiResponse.of(LessonResponse.from(lessonService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<LessonResponse> getById(@PathVariable Long id) {
        Lesson lesson = lessonService.getById(id);
        teacherScope.requireClass(lesson.getSchoolClassId());
        return ApiResponse.of(LessonResponse.from(lesson));
    }

    @GetMapping
    public ApiResponse<List<LessonResponse>> list(
            @RequestParam(required = false) Long schoolClassId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        if (schoolClassId != null && from != null && to != null) {
            teacherScope.requireClass(schoolClassId);
            List<LessonResponse> data = lessonService.listForClass(schoolClassId, from, to).stream()
                    .map(LessonResponse::from)
                    .toList();
            return ApiResponse.of(data);
        }
        if (schoolClassId != null || from != null || to != null) {
            throw ApiException.badRequest(
                    "MISSING_FILTER", "Fournir les trois paramètres schoolClassId, from et to ensemble", List.of());
        }
        var perimeter = teacherScope.currentPerimeter();
        Page<Lesson> page = perimeter.isPresent()
                ? lessonService.listForClasses(perimeter.get().classIds(), pageable)
                : lessonService.list(pageable);
        List<LessonResponse> data = page.map(LessonResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<LessonResponse> update(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        // Classe et matière ne changent pas à la mise à jour (colonnes non modifiables) :
        // c'est la séance existante qui fixe le périmètre.
        requireWritable(lessonService.getById(id));
        return ApiResponse.of(LessonResponse.from(lessonService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        requireWritable(lessonService.getById(id));
        lessonService.delete(id);
    }

    private void requireWritable(Lesson lesson) {
        teacherScope.requireClassSubject(lesson.getSchoolClassId(), lesson.getSubjectId());
    }
}
