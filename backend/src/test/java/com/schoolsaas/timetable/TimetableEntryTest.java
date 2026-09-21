package com.schoolsaas.timetable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** CRUD emploi du temps + détection de conflits (ROADMAP.md 1.6) + isolation cross-tenant. */
class TimetableEntryTest extends AbstractIntegrationTest {

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
    private TeacherRepository teacherRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    private record Fixture(Long classId, Long class2Id, Long subjectId, Long teacherId, Long roomId, String token) {
    }

    private Fixture setUpFixture(String label) throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, label);
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-" + tenant.getId() + "@ecole.example", Role.ADMIN);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        SchoolClass schoolClass2 = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème B", null), tenant.getId()));
        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "M-" + tenant.getId(), 3), tenant.getId()));
        Teacher teacher = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Jean", "Dupont", null, null), tenant.getId()));
        Room room = roomRepository.save(TestAuthSupport.withTenant(new Room("Salle 1", 30), tenant.getId()));
        return new Fixture(schoolClass.getId(), schoolClass2.getId(), subject.getId(), teacher.getId(), room.getId(), token);
    }

    private String entryJson(Fixture f, Long classId, String start, String end) {
        return "{\"schoolClassId\":" + classId + ",\"subjectId\":" + f.subjectId() + ",\"teacherId\":" + f.teacherId()
                + ",\"roomId\":" + f.roomId() + ",\"dayOfWeek\":\"MONDAY\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    @Test
    void createsATimetableEntry() throws Exception {
        Fixture f = setUpFixture("École Emploi du Temps");

        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + f.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(f, f.classId(), "08:00", "09:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dayOfWeek").value("MONDAY"));
    }

    @Test
    void rejectsOverlappingSlotForTheSameTeacher() throws Exception {
        Fixture f = setUpFixture("École Conflit Prof");
        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + f.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(f, f.classId(), "08:00", "09:00")))
                .andExpect(status().isCreated());

        // Même enseignant, classe différente, créneau qui chevauche : conflit enseignant.
        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + f.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(f, f.class2Id(), "08:30", "09:30")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TEACHER_ALREADY_BOOKED"));
    }

    @Test
    void rejectsOverlappingSlotForTheSameRoomEvenWithDifferentTeacher() throws Exception {
        Fixture f = setUpFixture("École Conflit Salle");
        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + f.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(f, f.classId(), "08:00", "09:00")))
                .andExpect(status().isCreated());

        Long tenantId = teacherRepository.findById(f.teacherId()).orElseThrow().getSchoolId();
        Teacher otherTeacher = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Autre", "Prof", null, null), tenantId));

        String body = "{\"schoolClassId\":" + f.class2Id() + ",\"subjectId\":" + f.subjectId()
                + ",\"teacherId\":" + otherTeacher.getId() + ",\"roomId\":" + f.roomId()
                + ",\"dayOfWeek\":\"MONDAY\",\"startTime\":\"08:30\",\"endTime\":\"09:30\"}";

        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + f.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROOM_ALREADY_BOOKED"));
    }

    @Test
    void nonOverlappingSlotsOnTheSameDayAreAllowed() throws Exception {
        Fixture f = setUpFixture("École Sans Conflit");
        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + f.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(f, f.classId(), "08:00", "09:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + f.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(f, f.classId(), "09:00", "10:00")))
                .andExpect(status().isCreated());
    }

    @Test
    void aTenantCanNeverSeeTimetableEntriesOfAnotherTenant() throws Exception {
        Fixture fA = setUpFixture("École ETT A");
        Fixture fB = setUpFixture("École ETT B");
        TimetableEntry entryB = timetableEntryRepository.save(TestAuthSupport.withTenant(
                new TimetableEntry(fB.classId(), fB.subjectId(), fB.teacherId(), fB.roomId(),
                        java.time.DayOfWeek.TUESDAY, java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0)),
                teacherRepository.findById(fB.teacherId()).orElseThrow().getSchoolId()));

        mockMvc.perform(get("/api/v1/timetable-entries/" + entryB.getId()).header("Authorization", "Bearer " + fA.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aTeacherCanReadTimetableEntriesButCannotCreateThem() throws Exception {
        Fixture f = setUpFixture("École EDT Enseignant");
        Long tenantId = teacherRepository.findById(f.teacherId()).orElseThrow().getSchoolId();
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc,
                objectMapper,
                userRepository,
                passwordEncoder,
                tenant,
                "teacher-edt@ecole.example",
                Role.TEACHER);

        mockMvc.perform(get("/api/v1/timetable-entries").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/timetable-entries")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(f, f.classId(), "11:00", "12:00")))
                .andExpect(status().isForbidden());
    }
}
