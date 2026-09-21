package com.schoolsaas.portal;

import com.schoolsaas.attendance.AttendanceService;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.grade.Grade;
import com.schoolsaas.grade.GradeRepository;
import com.schoolsaas.portal.dto.PortalResponse;
import com.schoolsaas.student.Student;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.timetable.Room;
import com.schoolsaas.timetable.RoomRepository;
import com.schoolsaas.timetable.TimetableEntry;
import com.schoolsaas.timetable.TimetableEntryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portail consulté par les familles depuis l'application mobile (cahier §13/§14).
 *
 * <p>Chaque méthode passe par {@link PortalService#requireAccessibleStudent} : le contrôle ne
 * repose pas sur {@code @PreAuthorize}, qui ne sait vérifier qu'un rôle, alors que la vraie
 * question ici est « cet élève appartient-il à cette famille ? ».
 */
@RestController
@RequestMapping("/api/v1/portal")
@PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
public class PortalController {

    private final PortalService portalService;
    private final AttendanceService attendanceService;
    private final GradeRepository gradeRepository;
    private final ExamRepository examRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;

    public PortalController(
            PortalService portalService,
            AttendanceService attendanceService,
            GradeRepository gradeRepository,
            ExamRepository examRepository,
            TimetableEntryRepository timetableEntryRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            RoomRepository roomRepository) {
        this.portalService = portalService;
        this.attendanceService = attendanceService;
        this.gradeRepository = gradeRepository;
        this.examRepository = examRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
    }

    /** Enfants du parent connecté, ou l'élève lui-même s'il consulte son propre portail. */
    @GetMapping("/children")
    public ApiResponse<List<PortalResponse.Child>> children() {
        return ApiResponse.of(portalService.accessibleStudents().stream()
                .map(PortalController::toChild)
                .toList());
    }

    @GetMapping("/students/{studentId}/grades")
    public ApiResponse<List<PortalResponse.GradeLine>> grades(@PathVariable Long studentId) {
        portalService.requireAccessibleStudent(studentId);

        List<Grade> grades = gradeRepository.findAllByStudentId(studentId);
        Map<Long, Exam> examsById = examRepository
                .findAllById(grades.stream().map(Grade::getExamId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));

        return ApiResponse.of(grades.stream()
                .filter(grade -> examsById.containsKey(grade.getExamId()))
                .map(grade -> {
                    Exam exam = examsById.get(grade.getExamId());
                    return new PortalResponse.GradeLine(
                            exam.getId(),
                            exam.getLabel(),
                            exam.getExamDate(),
                            grade.getScore(),
                            exam.getMaxScore(),
                            exam.getCoefficient(),
                            grade.isAbsent());
                })
                .sorted((a, b) -> b.examDate().compareTo(a.examDate()))
                .toList());
    }

    @GetMapping("/students/{studentId}/attendance")
    public ApiResponse<List<PortalResponse.AttendanceLine>> attendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        portalService.requireAccessibleStudent(studentId);

        return ApiResponse.of(attendanceService.listHistoryForStudent(studentId, from, to).stream()
                .map(record -> new PortalResponse.AttendanceLine(
                        record.getDate(),
                        record.getStatus().name(),
                        record.getReason(),
                        record.isJustified()))
                .toList());
    }

    /**
     * Emploi du temps de la classe de l'élève (mockup docs/MOCKUPS.md « Élève — emploi du
     * temps »). Les noms de matière, d'enseignant et de salle sont résolus ici : une famille
     * ne peut rien faire d'un identifiant technique.
     */
    @GetMapping("/students/{studentId}/timetable")
    public ApiResponse<List<PortalResponse.TimetableSlot>> timetable(@PathVariable Long studentId) {
        Student student = portalService.requireAccessibleStudent(studentId);
        if (student.getSchoolClassId() == null) {
            return ApiResponse.of(List.of());
        }

        List<TimetableEntry> entries =
                timetableEntryRepository.findAllBySchoolClassId(student.getSchoolClassId());
        Map<Long, String> subjects = subjectRepository
                .findAllById(entries.stream().map(TimetableEntry::getSubjectId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
        Map<Long, String> teachers = teacherRepository
                .findAllById(entries.stream().map(TimetableEntry::getTeacherId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(
                        Teacher::getId, teacher -> teacher.getFirstName() + " " + teacher.getLastName()));
        Map<Long, String> rooms = roomRepository
                .findAllById(entries.stream()
                        .map(TimetableEntry::getRoomId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Room::getId, Room::getName));

        return ApiResponse.of(entries.stream()
                .sorted(java.util.Comparator.comparing(TimetableEntry::getDayOfWeek)
                        .thenComparing(TimetableEntry::getStartTime))
                .map(entry -> new PortalResponse.TimetableSlot(
                        entry.getDayOfWeek().name(),
                        entry.getStartTime().toString(),
                        entry.getEndTime().toString(),
                        subjects.getOrDefault(entry.getSubjectId(), "Matière"),
                        teachers.getOrDefault(entry.getTeacherId(), ""),
                        entry.getRoomId() == null ? "" : rooms.getOrDefault(entry.getRoomId(), "")))
                .toList());
    }

    private static PortalResponse.Child toChild(Student student) {
        return new PortalResponse.Child(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getStudentNumber(),
                student.getSchoolClassId());
    }
}
