package com.schoolsaas.student;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.portal.FamilyAccountLookup;
import com.schoolsaas.student.dto.StudentImportResult;
import com.schoolsaas.student.dto.StudentRequest;
import com.schoolsaas.student.dto.StudentResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
// Créer, modifier, supprimer ou importer un élève reste au secrétariat et à la direction ;
// la LECTURE est ouverte à tout le personnel, car la liste des élèves est ce sur quoi
// travaillent la feuille d'appel, la saisie des notes, la discipline, la cantine, la
// bibliothèque et le transport. Le périmètre reste borné à l'établissement par RLS
// (docs/ARCHITECTURE.md ADR-001).
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
public class StudentController {

    private final StudentService studentService;
    private final FamilyAccountLookup familyAccountLookup;

    public StudentController(StudentService studentService, FamilyAccountLookup familyAccountLookup) {
        this.studentService = studentService;
        this.familyAccountLookup = familyAccountLookup;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.of(withPortalEmail(studentService.create(request)));
    }

    @PostMapping("/import")
    public ApiResponse<StudentImportResult> importCsv(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of(studentService.importCsv(file));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT')")
    public ApiResponse<StudentResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(withPortalEmail(studentService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT')")
    public ApiResponse<List<StudentResponse>> list(Pageable pageable, @RequestParam(required = false) String search) {
        Page<Student> page = studentService.list(pageable, search);
        Map<Long, String> portalEmails = familyAccountLookup.emailsOf(
                page.getContent().stream().map(Student::getUserId).toList());
        List<StudentResponse> data = page.map(record -> StudentResponse.from(record, portalEmails.get(record.getUserId()))).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.of(withPortalEmail(studentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        studentService.deactivate(id);
    }

    /** Complète la réponse avec l'adresse de connexion du compte famille, s'il en existe un. */
    private StudentResponse withPortalEmail(Student record) {
        return StudentResponse.from(record, familyAccountLookup.emailOf(record.getUserId()));
    }
}
