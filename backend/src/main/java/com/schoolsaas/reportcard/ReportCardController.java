package com.schoolsaas.reportcard;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.reportcard.dto.GenerateReportCardsRequest;
import com.schoolsaas.reportcard.dto.ReportCardEntryResponse;
import com.schoolsaas.reportcard.dto.ReportCardEntryUpdateRequest;
import com.schoolsaas.reportcard.dto.ReportCardResponse;
import com.schoolsaas.reportcard.dto.ReportCardUpdateRequest;
import com.schoolsaas.schoolclass.SchoolClassService;
import com.schoolsaas.student.StudentService;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bulletins scolaires — cahier-des-charges.md §12, ROADMAP.md 2.1. */
@RestController
@RequestMapping("/api/v1/report-cards")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER')")
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final ReportCardPdfExporter pdfExporter;
    private final StudentService studentService;
    private final SchoolClassService schoolClassService;
    private final SubjectRepository subjectRepository;
    private final TenantRepository tenantRepository;

    public ReportCardController(
            ReportCardService reportCardService,
            ReportCardPdfExporter pdfExporter,
            StudentService studentService,
            SchoolClassService schoolClassService,
            SubjectRepository subjectRepository,
            TenantRepository tenantRepository) {
        this.reportCardService = reportCardService;
        this.pdfExporter = pdfExporter;
        this.studentService = studentService;
        this.schoolClassService = schoolClassService;
        this.subjectRepository = subjectRepository;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<ReportCardResponse>> generate(@Valid @RequestBody GenerateReportCardsRequest request) {
        List<ReportCardResponse> data = reportCardService.generate(request).stream().map(this::toResponse).toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportCardResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(toResponse(reportCardService.getById(id)));
    }

    @GetMapping
    public ApiResponse<List<ReportCardResponse>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long schoolClassId,
            @RequestParam(required = false) String periodLabel) {
        List<ReportCard> reportCards;
        if (studentId != null) {
            reportCards = reportCardService.listForStudent(studentId);
        } else if (schoolClassId != null && periodLabel != null) {
            reportCards = reportCardService.listForClassAndPeriod(schoolClassId, periodLabel);
        } else {
            throw ApiException.badRequest(
                    "MISSING_FILTER", "Fournir studentId, ou (schoolClassId, periodLabel)", List.of());
        }
        return ApiResponse.of(reportCards.stream().map(this::toResponse).toList());
    }

    @PutMapping("/{id}")
    public ApiResponse<ReportCardResponse> update(@PathVariable Long id, @RequestBody ReportCardUpdateRequest request) {
        return ApiResponse.of(toResponse(reportCardService.update(id, request)));
    }

    @PutMapping("/{id}/entries/{subjectId}")
    public ApiResponse<ReportCardEntryResponse> updateEntry(
            @PathVariable Long id, @PathVariable Long subjectId, @RequestBody ReportCardEntryUpdateRequest request) {
        return ApiResponse.of(ReportCardEntryResponse.from(reportCardService.updateEntry(id, subjectId, request)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        ReportCard reportCard = reportCardService.getById(id);
        List<ReportCardEntry> entries = reportCardService.listEntries(id);
        Map<Long, Subject> subjectsById = subjectRepository
                .findAllById(entries.stream().map(ReportCardEntry::getSubjectId).toList())
                .stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        Tenant tenant = tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
        byte[] pdf = pdfExporter.export(
                reportCard,
                entries,
                studentService.getById(reportCard.getStudentId()),
                schoolClassService.getById(reportCard.getSchoolClassId()),
                subjectsById,
                tenant);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("bulletin-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    private ReportCardResponse toResponse(ReportCard reportCard) {
        List<ReportCardEntryResponse> entries = reportCardService.listEntries(reportCard.getId()).stream()
                .map(ReportCardEntryResponse::from)
                .toList();
        return ReportCardResponse.from(reportCard, entries);
    }
}
