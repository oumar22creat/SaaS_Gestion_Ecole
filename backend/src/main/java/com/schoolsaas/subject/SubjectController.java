package com.schoolsaas.subject;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.subject.dto.SubjectRequest;
import com.schoolsaas.subject.dto.SubjectResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD matières — cahier-des-charges.md §8, ROADMAP.md 1.5. */
@RestController
@RequestMapping("/api/v1/subjects")
// Écritures réservées à la direction ; lecture ouverte aux enseignants, qui ont besoin de la
// matière et de son coefficient pour créer une évaluation, saisir des notes et remplir le
// cahier de textes.
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.of(SubjectResponse.from(subjectService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
    public ApiResponse<SubjectResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(SubjectResponse.from(subjectService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
    public ApiResponse<List<SubjectResponse>> list(Pageable pageable) {
        Page<Subject> page = subjectService.list(pageable);
        List<SubjectResponse> data = page.map(SubjectResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<SubjectResponse> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.of(SubjectResponse.from(subjectService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        subjectService.delete(id);
    }
}
