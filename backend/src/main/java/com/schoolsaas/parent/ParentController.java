package com.schoolsaas.parent;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.parent.dto.ParentRequest;
import com.schoolsaas.parent.dto.ParentResponse;
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

/** CRUD parents/tuteurs — cahier-des-charges.md §7, ROADMAP.md 1.5. */
@RestController
@RequestMapping("/api/v1/parents")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
public class ParentController {

    private final ParentService parentService;
    private final StudentParentLinkService studentParentLinkService;

    public ParentController(ParentService parentService, StudentParentLinkService studentParentLinkService) {
        this.parentService = parentService;
        this.studentParentLinkService = studentParentLinkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParentResponse> create(@Valid @RequestBody ParentRequest request) {
        return ApiResponse.of(ParentResponse.from(parentService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ParentResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(ParentResponse.from(parentService.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<ParentResponse>> list(Pageable pageable, @RequestParam(required = false) String search) {
        Page<Parent> page = parentService.list(pageable, search);
        List<ParentResponse> data = page.map(ParentResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ParentResponse> update(@PathVariable Long id, @Valid @RequestBody ParentRequest request) {
        return ApiResponse.of(ParentResponse.from(parentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        parentService.delete(id);
    }

    @GetMapping("/{parentId}/students")
    public ApiResponse<List<Long>> studentsOf(@PathVariable Long parentId) {
        return ApiResponse.of(studentParentLinkService.listStudentIdsForParent(parentId));
    }
}
