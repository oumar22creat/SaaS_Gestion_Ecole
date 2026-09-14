package com.schoolsaas.schoolclass;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.schoolclass.dto.SchoolClassRequest;
import com.schoolsaas.schoolclass.dto.SchoolClassResponse;
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

/** CRUD classes — cahier-des-charges.md §8, ROADMAP.md 1.5. */
@RestController
@RequestMapping("/api/v1/classes")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SchoolClassResponse> create(@Valid @RequestBody SchoolClassRequest request) {
        return ApiResponse.of(SchoolClassResponse.from(schoolClassService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SchoolClassResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(SchoolClassResponse.from(schoolClassService.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<SchoolClassResponse>> list(Pageable pageable) {
        Page<SchoolClass> page = schoolClassService.list(pageable);
        List<SchoolClassResponse> data = page.map(SchoolClassResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<SchoolClassResponse> update(@PathVariable Long id, @Valid @RequestBody SchoolClassRequest request) {
        return ApiResponse.of(SchoolClassResponse.from(schoolClassService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        schoolClassService.delete(id);
    }
}
