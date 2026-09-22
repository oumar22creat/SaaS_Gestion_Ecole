package com.schoolsaas.attendance;

import com.schoolsaas.attendance.dto.AttendanceRecordRequest;
import com.schoolsaas.attendance.dto.RollCallRequest;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.notification.NotificationDispatcher;
import com.schoolsaas.notification.NotificationType;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.sms.SmsService;
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
    private final NotificationDispatcher notificationDispatcher;
    private final SmsService smsService;

    public AttendanceService(
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceRecordChangeRepository attendanceRecordChangeRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository,
            SmsService smsService,
            NotificationDispatcher notificationDispatcher) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceRecordChangeRepository = attendanceRecordChangeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.smsService = smsService;
        this.notificationDispatcher = notificationDispatcher;
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

    /**
     * Prévient les responsables (cahier-des-charges.md §10), par SMS : c'est le canal qui les
     * atteint réellement, tous n'ayant pas de smartphone. La notification interne est
     * conservée pour les comptes famille qui consultent le portail.
     *
     * <p>Le message nomme l'élève et sa classe. Il portait jusqu'ici son identifiant interne
     * (« Élève 7 — ABSENT ») : illisible pour un parent, et inutilisable pour une famille de
     * plusieurs enfants dans l'établissement.
     */
    private void notifyIfNeeded(AttendanceRecord record) {
        if (record.getStatus() == AttendanceStatus.PRESENT || record.isParentNotified()) {
            return;
        }
        String message = guardianMessage(record);
        notificationDispatcher.dispatch(NotificationType.ABSENCE, List.of(), "Absence signalée", message);
        smsService.notifyGuardians(record.getStudentId(), NotificationType.ABSENCE, message);
        record.setParentNotified(true);
    }

    private String guardianMessage(AttendanceRecord record) {
        String name = studentRepository.findById(record.getStudentId())
                .map(student -> student.getFirstName() + " " + student.getLastName())
                .orElse("Votre enfant");
        return name + " : " + statusLabel(record.getStatus()) + " le "
                + record.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".";
    }

    private static String statusLabel(AttendanceStatus status) {
        return switch (status) {
            case ABSENT -> "absence signalée";
            case LATE -> "retard signalé";
            case EARLY_DEPARTURE -> "départ anticipé signalé";
            case PRESENT -> "présence";
        };
    }
}
