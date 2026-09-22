package com.schoolsaas.teacher;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.teacher.dto.TeacherRequest;
import com.schoolsaas.teacher.dto.TeacherResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD enseignants — cahier-des-charges.md §8, ROADMAP.md 1.5. */
@RestController
@RequestMapping("/api/v1/teachers")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        return ApiResponse.of(TeacherResponse.from(teacherService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TeacherResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(TeacherResponse.from(teacherService.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<TeacherResponse>> list(Pageable pageable, @RequestParam(required = false) String search) {
        Page<Teacher> page = teacherService.list(pageable, search);
        List<TeacherResponse> data = page.map(TeacherResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<TeacherResponse> update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return ApiResponse.of(TeacherResponse.from(teacherService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        teacherService.deactivate(id);
    }
}
