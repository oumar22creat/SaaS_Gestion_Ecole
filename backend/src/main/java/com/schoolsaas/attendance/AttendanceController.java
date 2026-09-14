package com.schoolsaas.attendance;

import com.schoolsaas.attendance.dto.AttendanceRecordChangeResponse;
import com.schoolsaas.attendance.dto.AttendanceRecordRequest;
import com.schoolsaas.attendance.dto.AttendanceRecordResponse;
import com.schoolsaas.attendance.dto.RollCallRequest;
import com.schoolsaas.common.ApiResponse;
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

/** Feuille d'appel, motifs/justificatifs/historique — cahier-des-charges.md §10, ROADMAP.md 1.7. */
@RestController
@RequestMapping("/api/v1/attendance")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/roll-call")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<AttendanceRecordResponse>> submitRollCall(@Valid @RequestBody RollCallRequest request) {
        List<AttendanceRecordResponse> data =
                attendanceService.submitRollCall(request).stream().map(AttendanceRecordResponse::from).toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<AttendanceRecordResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(AttendanceRecordResponse.from(attendanceService.getById(id)));
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
            records = attendanceService.listForClassAndDate(schoolClassId, date);
        } else if (studentId != null && from != null && to != null) {
            records = attendanceService.listHistoryForStudent(studentId, from, to);
        } else {
            throw com.schoolsaas.common.ApiException.badRequest(
                    "MISSING_FILTER", "Fournir (schoolClassId, date) ou (studentId, from, to)", List.of());
        }
        return ApiResponse.of(records.stream().map(AttendanceRecordResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ApiResponse<AttendanceRecordResponse> update(@PathVariable Long id, @Valid @RequestBody AttendanceRecordRequest request) {
        return ApiResponse.of(AttendanceRecordResponse.from(attendanceService.update(id, request)));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<AttendanceRecordChangeResponse>> history(@PathVariable Long id) {
        return ApiResponse.of(attendanceService.listChanges(id).stream().map(AttendanceRecordChangeResponse::from).toList());
    }
}
