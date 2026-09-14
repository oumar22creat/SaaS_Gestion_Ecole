package com.schoolsaas.schoolclass;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.schoolclass.dto.ClassSubjectAssignmentRequest;
import com.schoolsaas.schoolclass.dto.ClassSubjectAssignmentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Affectation enseignant/classe/matière — cahier-des-charges.md §8, ROADMAP.md 1.5. */
@RestController
@RequestMapping("/api/v1/classes/{classId}/subjects")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class ClassSubjectAssignmentController {

    private final ClassSubjectAssignmentService assignmentService;

    public ClassSubjectAssignmentController(ClassSubjectAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassSubjectAssignmentResponse> assign(
            @PathVariable Long classId, @Valid @RequestBody ClassSubjectAssignmentRequest request) {
        return ApiResponse.of(ClassSubjectAssignmentResponse.from(assignmentService.assign(classId, request)));
    }

    @GetMapping
    public ApiResponse<List<ClassSubjectAssignmentResponse>> list(@PathVariable Long classId) {
        List<ClassSubjectAssignmentResponse> data = assignmentService.listForClass(classId).stream()
                .map(ClassSubjectAssignmentResponse::from)
                .toList();
        return ApiResponse.of(data);
    }

    @DeleteMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable Long classId, @PathVariable Long subjectId) {
        assignmentService.unassign(classId, subjectId);
    }
}
