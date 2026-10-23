package com.schoolsaas.teacher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.schoolsaas.homework.Lesson;
import com.schoolsaas.homework.LessonRepository;
import com.schoolsaas.notification.NotificationGateway;
import com.schoolsaas.reportcard.ReportCard;
import com.schoolsaas.reportcard.ReportCardRepository;
import com.schoolsaas.schoolclass.ClassSubjectAssignment;
import com.schoolsaas.schoolclass.ClassSubjectAssignmentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.timetable.TimetableEntry;
import com.schoolsaas.timetable.TimetableEntryRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Périmètre enseignant (ADR-030) : un enseignant n'accède qu'aux classes et matières qui lui
 * sont affectées, en lecture comme en écriture, sur l'appel, les notes, les bulletins,
 * l'emploi du temps et le cahier de textes. La direction et la vie scolaire gardent tout
 * l'établissement ; un utilisateur d'un autre établissement n'obtient rien.
 *
 * <p>Fixture commune : classe C1 (matières S1 et S2) et classe C2.
 * <ul>
 *   <li>« in » : fiche enseignant affectée à (C1, S1) via class_subject_assignments ;</li>
 *   <li>« out » : fiche enseignant de la même école, qui n'a qu'un créneau en (C2, S2) ;</li>
 *   <li>« head » : professeur principal de C1, sans aucune matière en C1.</li>
 * </ul>
 */
class TeacherScopeTest extends AbstractIntegrationTest {

    @MockBean
    private NotificationGateway notificationGateway;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ClassSubjectAssignmentRepository assignmentRepository;
    @Autowired private TimetableEntryRepository timetableEntryRepository;
    @Autowired private AttendanceRecordRepository attendanceRecordRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ReportCardRepository reportCardRepository;
    @Autowired private LessonRepository lessonRepository;

    private Tenant tenant;
    private SchoolClass c1;
    private SchoolClass c2;
    private Subject s1;
    private Subject s2;
    private Student studentC1;
    private Student studentC2;
    private Teacher teacherIn;
    private Teacher teacherOut;
    private TimetableEntry entryIn;
    private TimetableEntry entryOut;
    private String inEmail;
    private String tokenIn;
    private String tokenOut;
    private String tokenHead;
    private String tokenDirection;
    private String tokenVieScolaire;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Périmètre " + suffix);
        Long t = tenant.getId();

        inEmail = "prof-in-" + suffix + "@ecole.example";
        String outEmail = "prof-out-" + suffix + "@ecole.example";
        String headEmail = "prof-head-" + suffix + "@ecole.example";
        // Casse différente entre la fiche et le compte : le rattachement est insensible à la casse.
        teacherIn = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Ina", "Dedans", inEmail.toUpperCase(), null), t));
        teacherOut = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Otto", "Dehors", outEmail, null), t));
        Teacher head = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Hélène", "Principale", headEmail, null), t));

        c1 = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A " + suffix, head.getId()), t));
        c2 = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème B " + suffix, null), t));
        s1 = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "M" + suffix, 2), t));
        s2 = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Français", "F" + suffix, 2), t));
        studentC1 = studentRepository.save(TestAuthSupport.withTenant(new Student("C1-" + suffix, "Léa", "Martin", null, null, c1.getId()), t));
        studentC2 = studentRepository.save(TestAuthSupport.withTenant(new Student("C2-" + suffix, "Tom", "Durand", null, null, c2.getId()), t));

        assignmentRepository.save(TestAuthSupport.withTenant(new ClassSubjectAssignment(c1.getId(), s1.getId(), teacherIn.getId()), t));
        entryIn = timetableEntryRepository.save(TestAuthSupport.withTenant(new TimetableEntry(
                c1.getId(), s1.getId(), teacherIn.getId(), null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)), t));
        entryOut = timetableEntryRepository.save(TestAuthSupport.withTenant(new TimetableEntry(
                c2.getId(), s2.getId(), teacherOut.getId(), null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)), t));

        tokenIn = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder, tenant, inEmail, Role.TEACHER);
        tokenOut = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder, tenant, outEmail, Role.TEACHER);
        tokenHead = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder, tenant, headEmail, Role.TEACHER);
        tokenDirection = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "direction-" + suffix + "@ecole.example", Role.DIRECTION);
        tokenVieScolaire = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "vs-" + suffix + "@ecole.example", Role.VIE_SCOLAIRE);
    }

    // ---------------------------------------------------------------- Appel

    @Test
    void attendanceIsLimitedToTheTeachersClasses() throws Exception {
        String rollCall = rollCallBody(c1.getId(), studentC1.getId(), "2026-09-14");

        perform(post("/api/v1/attendance/roll-call").contentType(MediaType.APPLICATION_JSON).content(rollCall), tokenOut)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(TeacherScope.DENIED_CODE));
        perform(post("/api/v1/attendance/roll-call").contentType(MediaType.APPLICATION_JSON).content(rollCall), tokenIn)
                .andExpect(status().isCreated());

        AttendanceRecord record = attendanceRecordRepository.findAllBySchoolClassIdAndDate(c1.getId(), LocalDate.of(2026, 9, 14)).get(0);

        perform(get("/api/v1/attendance").param("schoolClassId", c1.getId().toString()).param("date", "2026-09-14"), tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        perform(get("/api/v1/attendance").param("schoolClassId", c1.getId().toString()).param("date", "2026-09-14"), tokenOut)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/attendance/" + record.getId()), tokenOut).andExpect(status().isForbidden());
        perform(get("/api/v1/attendance/" + record.getId() + "/history"), tokenOut).andExpect(status().isForbidden());
        perform(get("/api/v1/attendance").param("studentId", studentC1.getId().toString())
                .param("from", "2026-09-01").param("to", "2026-09-30"), tokenOut)
                .andExpect(status().isForbidden());
        perform(put("/api/v1/attendance/" + record.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ABSENT\",\"justified\":false}"), tokenOut)
                .andExpect(status().isForbidden());

        // Le professeur principal fait l'appel de sa classe, même sans y enseigner.
        perform(get("/api/v1/attendance/" + record.getId()), tokenHead).andExpect(status().isOk());

        // Périmètre établissement entier pour la direction et la vie scolaire.
        perform(get("/api/v1/attendance/" + record.getId()), tokenDirection).andExpect(status().isOk());
        perform(put("/api/v1/attendance/" + record.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LATE\",\"justified\":false}"), tokenVieScolaire)
                .andExpect(status().isOk());
        perform(post("/api/v1/attendance/roll-call").contentType(MediaType.APPLICATION_JSON)
                .content(rollCallBody(c2.getId(), studentC2.getId(), "2026-09-14")), tokenDirection)
                .andExpect(status().isCreated());
    }

    @Test
    void aTeacherCannotSlipAStudentOfAnotherClassIntoHisRollCall() throws Exception {
        perform(post("/api/v1/attendance/roll-call").contentType(MediaType.APPLICATION_JSON)
                .content(rollCallBody(c1.getId(), studentC2.getId(), "2026-09-15")), tokenIn)
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- Notes

    @Test
    void examsAndGradesAreLimitedToTheTeachersClassSubjectPairs() throws Exception {
        Exam examIn = examRepository.save(TestAuthSupport.withTenant(
                new Exam(c1.getId(), s1.getId(), "Devoir maths", 20, 1, LocalDate.of(2026, 10, 1)), tenant.getId()));
        // Même classe, autre matière : hors périmètre de « in », pourtant affecté à C1.
        Exam examOtherSubject = examRepository.save(TestAuthSupport.withTenant(
                new Exam(c1.getId(), s2.getId(), "Dictée", 20, 1, LocalDate.of(2026, 10, 2)), tenant.getId()));

        String grades = "{\"entries\":[{\"studentId\":" + studentC1.getId() + ",\"score\":14,\"absent\":false}]}";

        perform(get("/api/v1/exams/" + examIn.getId()), tokenIn).andExpect(status().isOk());
        perform(post("/api/v1/exams/" + examIn.getId() + "/grades").contentType(MediaType.APPLICATION_JSON).content(grades), tokenIn)
                .andExpect(status().isCreated());
        perform(get("/api/v1/exams/" + examIn.getId() + "/grades"), tokenIn).andExpect(status().isOk());
        perform(get("/api/v1/exams"), tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(examIn.getId()));

        perform(get("/api/v1/exams/" + examOtherSubject.getId()), tokenIn).andExpect(status().isForbidden());
        perform(get("/api/v1/exams/" + examIn.getId()), tokenOut).andExpect(status().isForbidden());
        perform(get("/api/v1/exams/" + examIn.getId() + "/grades"), tokenOut).andExpect(status().isForbidden());
        perform(get("/api/v1/exams/" + examIn.getId() + "/statistics"), tokenOut).andExpect(status().isForbidden());
        perform(post("/api/v1/exams/" + examIn.getId() + "/grades").contentType(MediaType.APPLICATION_JSON).content(grades), tokenOut)
                .andExpect(status().isForbidden());
        perform(put("/api/v1/exams/" + examIn.getId() + "/grades/" + studentC1.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":" + studentC1.getId() + ",\"score\":2,\"absent\":false}"), tokenOut)
                .andExpect(status().isForbidden());
        perform(delete("/api/v1/exams/" + examIn.getId()), tokenOut).andExpect(status().isForbidden());
        perform(post("/api/v1/exams").contentType(MediaType.APPLICATION_JSON).content(examBody(c1.getId(), s1.getId())), tokenOut)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/classes/" + c1.getId() + "/subjects/" + s1.getId() + "/average"), tokenOut)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/students/" + studentC1.getId() + "/subjects/" + s1.getId() + "/average"), tokenOut)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/exams"), tokenOut)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        // Professeur principal : sa classe, mais pas les notes d'une matière qu'il n'enseigne pas.
        perform(get("/api/v1/exams/" + examIn.getId() + "/grades"), tokenHead).andExpect(status().isForbidden());

        perform(post("/api/v1/exams").contentType(MediaType.APPLICATION_JSON).content(examBody(c1.getId(), s1.getId())), tokenIn)
                .andExpect(status().isCreated());
        perform(get("/api/v1/students/" + studentC1.getId() + "/subjects/" + s1.getId() + "/average"), tokenIn)
                .andExpect(status().isOk());

        perform(get("/api/v1/exams/" + examOtherSubject.getId() + "/grades"), tokenDirection).andExpect(status().isOk());
        perform(get("/api/v1/exams"), tokenDirection)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void aTeacherCannotGradeAStudentOutsideTheExamsClass() throws Exception {
        Exam examIn = examRepository.save(TestAuthSupport.withTenant(
                new Exam(c1.getId(), s1.getId(), "Interro", 20, 1, LocalDate.of(2026, 10, 3)), tenant.getId()));
        perform(post("/api/v1/exams/" + examIn.getId() + "/grades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"entries\":[{\"studentId\":" + studentC2.getId() + ",\"score\":10,\"absent\":false}]}"), tokenIn)
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- Bulletins

    @Test
    void reportCardsAreLimitedToTheTeachersClasses() throws Exception {
        ReportCard card = reportCardRepository.save(TestAuthSupport.withTenant(
                new ReportCard(studentC1.getId(), c1.getId(), "T1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20)),
                tenant.getId()));
        String update = "{\"generalComment\":\"Bon trimestre\",\"councilDecision\":null}";

        perform(get("/api/v1/report-cards/" + card.getId()), tokenIn).andExpect(status().isOk());
        perform(get("/api/v1/report-cards").param("schoolClassId", c1.getId().toString()).param("periodLabel", "T1"), tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        perform(put("/api/v1/report-cards/" + card.getId()).contentType(MediaType.APPLICATION_JSON).content(update), tokenHead)
                .andExpect(status().isOk());

        perform(get("/api/v1/report-cards/" + card.getId()), tokenOut).andExpect(status().isForbidden());
        perform(get("/api/v1/report-cards").param("schoolClassId", c1.getId().toString()).param("periodLabel", "T1"), tokenOut)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/report-cards").param("studentId", studentC1.getId().toString()), tokenOut)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/report-cards/" + card.getId() + "/pdf"), tokenOut).andExpect(status().isForbidden());
        perform(put("/api/v1/report-cards/" + card.getId()).contentType(MediaType.APPLICATION_JSON).content(update), tokenOut)
                .andExpect(status().isForbidden());
        perform(put("/api/v1/report-cards/" + card.getId() + "/entries/" + s2.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"teacherComment\":\"x\"}"), tokenIn)
                .andExpect(status().isForbidden());
        perform(post("/api/v1/report-cards/generate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"schoolClassId\":" + c1.getId() + ",\"periodLabel\":\"T1\",\"periodFrom\":\"2026-09-01\",\"periodTo\":\"2026-12-20\"}"),
                tokenOut)
                .andExpect(status().isForbidden());

        perform(get("/api/v1/report-cards/" + card.getId()), tokenDirection).andExpect(status().isOk());
        perform(put("/api/v1/report-cards/" + card.getId()).contentType(MediaType.APPLICATION_JSON).content(update), tokenDirection)
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- Emploi du temps

    @Test
    void aTeacherOnlySeesHisOwnTimetable() throws Exception {
        perform(get("/api/v1/timetable-entries"), tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(entryIn.getId()));
        perform(get("/api/v1/timetable-entries").param("teacherId", teacherIn.getId().toString()), tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        perform(get("/api/v1/timetable-entries/" + entryIn.getId()), tokenIn).andExpect(status().isOk());

        perform(get("/api/v1/timetable-entries").param("teacherId", teacherOut.getId().toString()), tokenIn)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(TeacherScope.DENIED_CODE));
        perform(get("/api/v1/timetable-entries/" + entryOut.getId()), tokenIn).andExpect(status().isForbidden());
        // Filtre par classe : seulement SES créneaux de cette classe, jamais ceux d'un collègue.
        perform(get("/api/v1/timetable-entries").param("schoolClassId", c2.getId().toString()), tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        perform(get("/api/v1/timetable-entries"), tokenDirection)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        perform(get("/api/v1/timetable-entries").param("teacherId", teacherOut.getId().toString()), tokenDirection)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ---------------------------------------------------------------- Cahier de textes

    @Test
    void lessonsAreReadableForTheTeachersClassesAndWritableForHisSubjects() throws Exception {
        Lesson lesson = lessonRepository.save(TestAuthSupport.withTenant(
                new Lesson(c1.getId(), s1.getId(), LocalDate.of(2026, 10, 5), "Fractions"), tenant.getId()));

        perform(post("/api/v1/lessons").contentType(MediaType.APPLICATION_JSON).content(lessonBody(c1.getId(), s1.getId())), tokenIn)
                .andExpect(status().isCreated());
        perform(post("/api/v1/lessons").contentType(MediaType.APPLICATION_JSON).content(lessonBody(c1.getId(), s2.getId())), tokenIn)
                .andExpect(status().isForbidden());
        perform(post("/api/v1/lessons").contentType(MediaType.APPLICATION_JSON).content(lessonBody(c1.getId(), s1.getId())), tokenOut)
                .andExpect(status().isForbidden());

        perform(get("/api/v1/lessons/" + lesson.getId()), tokenHead).andExpect(status().isOk());
        perform(put("/api/v1/lessons/" + lesson.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(lessonBody(c1.getId(), s1.getId())), tokenHead)
                .andExpect(status().isForbidden());

        perform(get("/api/v1/lessons/" + lesson.getId()), tokenOut).andExpect(status().isForbidden());
        perform(get("/api/v1/lessons").param("schoolClassId", c1.getId().toString())
                .param("from", "2026-10-01").param("to", "2026-10-31"), tokenOut)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/lessons"), tokenOut)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        perform(put("/api/v1/lessons/" + lesson.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(lessonBody(c1.getId(), s1.getId())), tokenOut)
                .andExpect(status().isForbidden());
        perform(delete("/api/v1/lessons/" + lesson.getId()), tokenOut).andExpect(status().isForbidden());

        perform(get("/api/v1/lessons/" + lesson.getId()), tokenDirection).andExpect(status().isOk());
        perform(delete("/api/v1/lessons/" + lesson.getId()), tokenIn).andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------- Classes, compte sans fiche

    @Test
    void theClassListOfATeacherOnlyContainsHisClasses() throws Exception {
        perform(get("/api/v1/classes"), tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(c1.getId()));
        perform(get("/api/v1/classes"), tokenDirection)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void aTeacherAccountWithoutMatchingTeacherRecordHasAnEmptyPerimeter() throws Exception {
        String orphan = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sans-fiche-" + UUID.randomUUID() + "@ecole.example",
                Role.TEACHER);
        perform(get("/api/v1/attendance").param("schoolClassId", c1.getId().toString()).param("date", "2026-09-14"), orphan)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/timetable-entries"), orphan)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---------------------------------------------------------------- Isolation tenant

    @Test
    void aTeacherOfAnotherTenantWithTheSameEmailGetsNothing() throws Exception {
        Tenant other = TestAuthSupport.createActiveTenant(tenantRepository, "École Voisine " + UUID.randomUUID());
        // Même adresse que la fiche « in » de l'autre établissement : le rattachement par e-mail
        // ne doit JAMAIS traverser la frontière du tenant.
        String tokenOther = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, other, inEmail, Role.TEACHER);
        Exam examIn = examRepository.save(TestAuthSupport.withTenant(
                new Exam(c1.getId(), s1.getId(), "Devoir", 20, 1, LocalDate.of(2026, 10, 1)), tenant.getId()));

        perform(get("/api/v1/attendance").param("schoolClassId", c1.getId().toString()).param("date", "2026-09-14"), tokenOther)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/exams/" + examIn.getId()), tokenOther).andExpect(status().isNotFound());
        perform(get("/api/v1/timetable-entries/" + entryIn.getId()), tokenOther).andExpect(status().isNotFound());
        perform(get("/api/v1/timetable-entries"), tokenOther)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        perform(get("/api/v1/classes"), tokenOther)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        String directionOther = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, other, "dir-" + UUID.randomUUID() + "@ecole.example", Role.DIRECTION);
        perform(get("/api/v1/exams/" + examIn.getId()), directionOther).andExpect(status().isNotFound());
        perform(get("/api/v1/timetable-entries"), directionOther)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---------------------------------------------------------------- Outils

    private ResultActions perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String token)
            throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private static String rollCallBody(Long classId, Long studentId, String date) {
        return "{\"schoolClassId\":" + classId + ",\"date\":\"" + date + "\",\"entries\":["
                + "{\"studentId\":" + studentId + ",\"status\":\"PRESENT\",\"justified\":false}]}";
    }

    private static String examBody(Long classId, Long subjectId) {
        return "{\"schoolClassId\":" + classId + ",\"subjectId\":" + subjectId
                + ",\"label\":\"Contrôle\",\"maxScore\":20,\"coefficient\":1,\"examDate\":\"2026-10-10\"}";
    }

    private static String lessonBody(Long classId, Long subjectId) {
        return "{\"schoolClassId\":" + classId + ",\"subjectId\":" + subjectId
                + ",\"sessionDate\":\"2026-10-06\",\"content\":\"Exercices\"}";
    }
}
