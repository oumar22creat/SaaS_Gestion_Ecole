package com.schoolsaas.grade;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.grade.dto.ExamRequest;
import com.schoolsaas.grade.dto.ExamResponse;
import com.schoolsaas.grade.dto.ExamStatisticsResponse;
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

/** CRUD évaluations — cahier-des-charges.md §11, ROADMAP.md 1.8. */
@RestController
@RequestMapping("/api/v1/exams")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
public class ExamController {

    private final ExamService examService;
    private final GradeService gradeService;

    public ExamController(ExamService examService, GradeService gradeService) {
        this.examService = examService;
        this.gradeService = gradeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExamResponse> create(@Valid @RequestBody ExamRequest request) {
        return ApiResponse.of(ExamResponse.from(examService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(ExamResponse.from(examService.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<ExamResponse>> list(Pageable pageable) {
        Page<Exam> page = examService.list(pageable);
        List<ExamResponse> data = page.map(ExamResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ExamResponse> update(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.of(ExamResponse.from(examService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        examService.delete(id);
    }

    @GetMapping("/{id}/statistics")
    public ApiResponse<ExamStatisticsResponse> statistics(@PathVariable Long id) {
        return ApiResponse.of(gradeService.examStatistics(id));
    }
}
