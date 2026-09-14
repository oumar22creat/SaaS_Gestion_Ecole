package com.schoolsaas.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.attendance.AttendanceRecord;
import com.schoolsaas.attendance.AttendanceRecordRepository;
import com.schoolsaas.attendance.AttendanceStatus;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.grade.Grade;
import com.schoolsaas.grade.GradeRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Tableau de bord établissement — statistiques de base (ROADMAP.md 1.9). */
class DashboardTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Test
    void computesBasicDashboardStatistics() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Dashboard");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-dashboard@ecole.example", Role.ADMIN);

        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Jean", "Dupont", null, null), tenant.getId()));
        Student s1 = studentRepository.save(TestAuthSupport.withTenant(new Student("D1", "Ada", "L", null, null, schoolClass.getId()), tenant.getId()));
        Student s2 = studentRepository.save(TestAuthSupport.withTenant(new Student("D2", "Alan", "T", null, null, schoolClass.getId()), tenant.getId()));

        LocalDate today = LocalDate.now();
        attendanceRecordRepository.save(TestAuthSupport.withTenant(
                new AttendanceRecord(s1.getId(), schoolClass.getId(), today, AttendanceStatus.PRESENT, null, false, null), tenant.getId()));
        attendanceRecordRepository.save(TestAuthSupport.withTenant(
                new AttendanceRecord(s2.getId(), schoolClass.getId(), today, AttendanceStatus.ABSENT, "Maladie", true, null), tenant.getId()));

        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH", 1), tenant.getId()));
        Exam exam = examRepository.save(TestAuthSupport.withTenant(
                new Exam(schoolClass.getId(), subject.getId(), "Contrôle", 20, 1, today), tenant.getId()));
        gradeRepository.save(TestAuthSupport.withTenant(new Grade(exam.getId(), s1.getId(), 16.0, false, null), tenant.getId()));
        gradeRepository.save(TestAuthSupport.withTenant(new Grade(exam.getId(), s2.getId(), 8.0, false, null), tenant.getId()));

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentCount").value(2))
                .andExpect(jsonPath("$.data.teacherCount").value(1))
                .andExpect(jsonPath("$.data.classCount").value(1))
                .andExpect(jsonPath("$.data.attendanceRate").value(50.0))
                .andExpect(jsonPath("$.data.averageGrade").value(12.0));
    }

    @Test
    void teacherRoleCannotAccessTheDashboard() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Dashboard RBAC");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-dashboard@ecole.example", Role.TEACHER);

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void aTenantsDashboardNeverIncludesAnotherTenantsData() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Dashboard A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Dashboard B");
        studentRepository.save(TestAuthSupport.withTenant(new Student("X1", "Isolé", "TenantB", null, null, null), tenantB.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("X2", "Isolé2", "TenantB", null, null, null), tenantB.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("X3", "Isolé3", "TenantB", null, null, null), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-dashboard@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentCount").value(0));
    }
}
