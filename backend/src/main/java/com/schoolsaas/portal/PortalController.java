package com.schoolsaas.portal;

import com.schoolsaas.attendance.AttendanceService;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.grade.Grade;
import com.schoolsaas.grade.GradeRepository;
import com.schoolsaas.portal.dto.PortalResponse;
import com.schoolsaas.student.Student;
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

    public PortalController(
            PortalService portalService,
            AttendanceService attendanceService,
            GradeRepository gradeRepository,
            ExamRepository examRepository) {
        this.portalService = portalService;
        this.attendanceService = attendanceService;
        this.gradeRepository = gradeRepository;
        this.examRepository = examRepository;
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

    private static PortalResponse.Child toChild(Student student) {
        return new PortalResponse.Child(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getStudentNumber(),
                student.getSchoolClassId());
    }
}
