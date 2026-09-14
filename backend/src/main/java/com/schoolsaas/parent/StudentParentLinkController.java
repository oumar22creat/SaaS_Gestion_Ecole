package com.schoolsaas.parent;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.parent.dto.StudentParentLinkRequest;
import com.schoolsaas.parent.dto.StudentParentLinkResponse;
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

/** Association élève/parent — cahier-des-charges.md §7, ROADMAP.md 1.5. */
@RestController
@RequestMapping("/api/v1/students/{studentId}/parents")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
public class StudentParentLinkController {

    private final StudentParentLinkService studentParentLinkService;

    public StudentParentLinkController(StudentParentLinkService studentParentLinkService) {
        this.studentParentLinkService = studentParentLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentParentLinkResponse> link(
            @PathVariable Long studentId, @Valid @RequestBody StudentParentLinkRequest request) {
        return ApiResponse.of(StudentParentLinkResponse.from(studentParentLinkService.link(studentId, request)));
    }

    @GetMapping
    public ApiResponse<List<StudentParentLinkResponse>> list(@PathVariable Long studentId) {
        List<StudentParentLinkResponse> data =
                studentParentLinkService.listForStudent(studentId).stream().map(StudentParentLinkResponse::from).toList();
        return ApiResponse.of(data);
    }

    @DeleteMapping("/{parentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable Long studentId, @PathVariable Long parentId) {
        studentParentLinkService.unlink(studentId, parentId);
    }
}
