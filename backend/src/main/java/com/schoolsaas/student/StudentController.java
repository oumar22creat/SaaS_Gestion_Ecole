package com.schoolsaas.student;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.student.dto.StudentImportResult;
import com.schoolsaas.student.dto.StudentRequest;
import com.schoolsaas.student.dto.StudentResponse;
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
import org.springframework.web.multipart.MultipartFile;

/** CRUD élèves + import CSV — cahier-des-charges.md §7, ROADMAP.md 1.5. */
@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.of(StudentResponse.from(studentService.create(request)));
    }

    @PostMapping("/import")
    public ApiResponse<StudentImportResult> importCsv(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of(studentService.importCsv(file));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(StudentResponse.from(studentService.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<StudentResponse>> list(Pageable pageable) {
        Page<Student> page = studentService.list(pageable);
        List<StudentResponse> data = page.map(StudentResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.of(StudentResponse.from(studentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        studentService.deactivate(id);
    }
}
