package com.schoolsaas.grade;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.grade.dto.ClassSubjectAverageResponse;
import com.schoolsaas.grade.dto.GradeEntryRequest;
import com.schoolsaas.grade.dto.GradeResponse;
import com.schoolsaas.grade.dto.GradeSubmissionRequest;
import com.schoolsaas.grade.dto.SubjectAverageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Saisie des notes + calcul automatique des moyennes — cahier-des-charges.md §11, ROADMAP.md 1.8. */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping("/api/v1/exams/{examId}/grades")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<GradeResponse>> submit(@PathVariable Long examId, @Valid @RequestBody GradeSubmissionRequest request) {
        List<GradeResponse> data = gradeService.submit(examId, request.entries()).stream().map(GradeResponse::from).toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/api/v1/exams/{examId}/grades")
    public ApiResponse<List<GradeResponse>> list(@PathVariable Long examId) {
        return ApiResponse.of(gradeService.listForExam(examId).stream().map(GradeResponse::from).toList());
    }

    @PutMapping("/api/v1/exams/{examId}/grades/{studentId}")
    public ApiResponse<GradeResponse> update(
            @PathVariable Long examId, @PathVariable Long studentId, @Valid @RequestBody GradeEntryRequest request) {
        return ApiResponse.of(GradeResponse.from(gradeService.updateOne(examId, studentId, request)));
    }

    @GetMapping("/api/v1/students/{studentId}/subjects/{subjectId}/average")
    public ApiResponse<SubjectAverageResponse> studentSubjectAverage(@PathVariable Long studentId, @PathVariable Long subjectId) {
        return ApiResponse.of(gradeService.studentSubjectAverage(studentId, subjectId));
    }

    @GetMapping("/api/v1/classes/{schoolClassId}/subjects/{subjectId}/average")
    public ApiResponse<ClassSubjectAverageResponse> classSubjectAverage(
            @PathVariable Long schoolClassId, @PathVariable Long subjectId) {
        return ApiResponse.of(gradeService.classSubjectAverage(schoolClassId, subjectId));
    }
}
