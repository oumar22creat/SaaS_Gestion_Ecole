package com.schoolsaas.attendance;

import com.schoolsaas.attendance.dto.AttendanceRecordChangeResponse;
import com.schoolsaas.attendance.dto.AttendanceRecordRequest;
import com.schoolsaas.attendance.dto.AttendanceRecordResponse;
import com.schoolsaas.attendance.dto.RollCallRequest;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.teacher.TeacherScope;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

/**
 * Feuille d'appel, motifs/justificatifs/historique — cahier-des-charges.md §10, ROADMAP.md 1.7.
 *
 * <p>Un enseignant n'accède qu'aux classes de son périmètre ({@link TeacherScope}, ADR-030),
 * en lecture comme en écriture ; la direction et la vie scolaire voient tout l'établissement.
 */
@RestController
@RequestMapping("/api/v1/attendance")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final TeacherScope teacherScope;

    public AttendanceController(AttendanceService attendanceService, TeacherScope teacherScope) {
        this.attendanceService = attendanceService;
        this.teacherScope = teacherScope;
    }

    @PostMapping("/roll-call")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<AttendanceRecordResponse>> submitRollCall(@Valid @RequestBody RollCallRequest request) {
        // Classe du périmètre ET chaque élève inscrit dans cette classe : la classe seule ne
        // suffit pas, un enseignant pourrait glisser dans l'appel de SA classe un élève d'une
        // autre classe et modifier sa présence du jour.
        teacherScope.requireStudentsInClass(
                request.entries().stream().map(RollCallRequest.Entry::studentId).toList(), request.schoolClassId());
        List<AttendanceRecordResponse> data =
                attendanceService.submitRollCall(request).stream().map(AttendanceRecordResponse::from).toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<AttendanceRecordResponse> getById(@PathVariable Long id) {
        AttendanceRecord record = attendanceService.getById(id);
        teacherScope.requireClass(record.getSchoolClassId());
        return ApiResponse.of(AttendanceRecordResponse.from(record));
    }

    @GetMapping
    public ApiResponse<List<AttendanceRecordResponse>> list(
            @RequestParam(required = false) Long schoolClassId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AttendanceRecord> records;
        if (schoolClassId != null && date != null) {
            teacherScope.requireClass(schoolClassId);
            records = attendanceService.listForClassAndDate(schoolClassId, date);
        } else if (studentId != null && from != null && to != null) {
            teacherScope.requireStudent(studentId);
            records = attendanceService.listHistoryForStudent(studentId, from, to);
        } else {
            throw com.schoolsaas.common.ApiException.badRequest(
                    "MISSING_FILTER", "Fournir (schoolClassId, date) ou (studentId, from, to)", List.of());
        }
        return ApiResponse.of(records.stream().map(AttendanceRecordResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ApiResponse<AttendanceRecordResponse> update(@PathVariable Long id, @Valid @RequestBody AttendanceRecordRequest request) {
        teacherScope.requireClass(attendanceService.getById(id).getSchoolClassId());
        return ApiResponse.of(AttendanceRecordResponse.from(attendanceService.update(id, request)));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<AttendanceRecordChangeResponse>> history(@PathVariable Long id) {
        teacherScope.requireClass(attendanceService.getById(id).getSchoolClassId());
        return ApiResponse.of(attendanceService.listChanges(id).stream().map(AttendanceRecordChangeResponse::from).toList());
    }
}
