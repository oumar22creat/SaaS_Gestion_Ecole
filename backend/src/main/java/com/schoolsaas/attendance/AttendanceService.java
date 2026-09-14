package com.schoolsaas.attendance;

import com.schoolsaas.attendance.dto.AttendanceRecordRequest;
import com.schoolsaas.attendance.dto.RollCallRequest;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Feuille d'appel, motifs/justificatifs/historique, notification parent — ROADMAP.md 1.7. */
@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceRecordChangeRepository attendanceRecordChangeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final ParentNotificationGateway parentNotificationGateway;

    public AttendanceService(
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceRecordChangeRepository attendanceRecordChangeRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository,
            ParentNotificationGateway parentNotificationGateway) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceRecordChangeRepository = attendanceRecordChangeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.parentNotificationGateway = parentNotificationGateway;
    }

    @Transactional
    public List<AttendanceRecord> submitRollCall(RollCallRequest request) {
        if (schoolClassRepository.findById(request.schoolClassId()).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
        return request.entries().stream()
                .map(entry -> upsert(request.schoolClassId(), request.date(), entry))
                .toList();
    }

    private AttendanceRecord upsert(Long schoolClassId, LocalDate date, RollCallRequest.Entry entry) {
        if (studentRepository.findById(entry.studentId()).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable : " + entry.studentId());
        }
        return attendanceRecordRepository.findByStudentIdAndDate(entry.studentId(), date)
                .map(existing -> applyChange(existing, entry.status(), entry.reason(), entry.justified(), entry.comment()))
                .orElseGet(() -> createAndNotifyIfNeeded(new AttendanceRecord(
                        entry.studentId(), schoolClassId, date, entry.status(), entry.reason(), entry.justified(),
                        entry.comment())));
    }

    public AttendanceRecord getById(Long id) {
        return attendanceRecordRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ATTENDANCE_RECORD_NOT_FOUND", "Enregistrement introuvable"));
    }

    public List<AttendanceRecord> listForClassAndDate(Long schoolClassId, LocalDate date) {
        return attendanceRecordRepository.findAllBySchoolClassIdAndDate(schoolClassId, date);
    }

    public List<AttendanceRecord> listHistoryForStudent(Long studentId, LocalDate from, LocalDate to) {
        return attendanceRecordRepository.findAllByStudentIdAndDateBetween(studentId, from, to);
    }

    public List<AttendanceRecordChange> listChanges(Long attendanceRecordId) {
        return attendanceRecordChangeRepository.findAllByAttendanceRecordIdOrderByChangedAtDesc(attendanceRecordId);
    }

    @Transactional
    public AttendanceRecord update(Long id, AttendanceRecordRequest request) {
        AttendanceRecord record = getById(id);
        return applyChange(record, request.status(), request.reason(), request.justified(), request.comment());
    }

    private AttendanceRecord applyChange(
            AttendanceRecord record, AttendanceStatus newStatus, String newReason, boolean newJustified, String newComment) {
        attendanceRecordChangeRepository.save(new AttendanceRecordChange(
                record.getId(), record.getStatus(), record.getReason(), record.isJustified(), record.getComment()));
        record.setStatus(newStatus);
        record.setReason(newReason);
        record.setJustified(newJustified);
        record.setComment(newComment);
        notifyIfNeeded(record);
        return record;
    }

    private AttendanceRecord createAndNotifyIfNeeded(AttendanceRecord record) {
        AttendanceRecord saved = attendanceRecordRepository.save(record);
        notifyIfNeeded(saved);
        return saved;
    }

    /** Notification au parent (cahier-des-charges.md §10) — voir ParentNotificationGateway. */
    private void notifyIfNeeded(AttendanceRecord record) {
        if (record.getStatus() != AttendanceStatus.PRESENT && !record.isParentNotified()) {
            parentNotificationGateway.notifyAbsence(record.getStudentId(), record.getDate(), record.getStatus());
            record.setParentNotified(true);
        }
    }
}
