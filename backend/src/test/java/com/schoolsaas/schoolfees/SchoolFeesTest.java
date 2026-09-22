package com.schoolsaas.schoolfees;

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
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Comptabilité et frais scolaires (ROADMAP.md 3.3) : grille tarifaire, génération de factures, paiements, reporting, RBAC, isolation. */
class SchoolFeesTest extends AbstractIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private FeeScheduleRepository feeScheduleRepository;

    @Test
    void generatesInvoicesForActiveStudentsAndTracksPaymentsToFullSettlement() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Frais Scolaires");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("F1", "Ada", "L", null, null, schoolClass.getId()), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("F2", "Alan", "T", null, null, schoolClass.getId()), tenant.getId()));
        Student inactive = studentRepository.save(
                TestAuthSupport.withTenant(new Student("F3", "Grace", "H", null, null, schoolClass.getId()), tenant.getId()));
        inactive.setActive(false);
        studentRepository.save(inactive);

        String accountantToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-fees@ecole.example", Role.ACCOUNTANT);
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-fees@ecole.example", Role.TEACHER);

        mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"label\":\"Scolarité 2026\",\"amountCents\":100000,"
                                + "\"dueDate\":\"2026-10-01\"}"))
                .andExpect(status().isForbidden());

        String scheduleResp = mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"label\":\"Scolarité 2026\",\"amountCents\":100000,"
                                + "\"dueDate\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long scheduleId = objectMapper.readTree(scheduleResp).get("data").get("id").asLong();

        String invoicesResp = mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleId + "/generate-invoices")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Long invoiceId = objectMapper.readTree(invoicesResp).get("data").get(0).get("id").asLong();

        // Régénérer est idempotent : toujours 2 factures, pas de doublon pour l'élève inactif.
        mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleId + "/generate-invoices")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post("/api/v1/school-fees/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":40000,\"method\":\"CASH\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/school-fees/invoices/" + invoiceId).header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.data.amountPaidCents").value(40000));

        mockMvc.perform(post("/api/v1/school-fees/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":60000,\"method\":\"MOBILE_MONEY\",\"reference\":\"OM-12345\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/school-fees/invoices/" + invoiceId).header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amountPaidCents").value(100000));

        mockMvc.perform(get("/api/v1/school-fees/reporting")
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("from", "2026-09-01")
                        .param("to", "2026-10-31")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDueCents").value(200000))
                .andExpect(jsonPath("$.data.totalPaidCents").value(100000))
                .andExpect(jsonPath("$.data.totalOutstandingCents").value(100000))
                .andExpect(jsonPath("$.data.unpaidInvoices.length()").value(1));
    }

    @Test
    void aTenantCanNeverSeeFeeSchedulesOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Frais A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Frais B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        FeeSchedule scheduleB = feeScheduleRepository.save(TestAuthSupport.withTenant(
                new FeeSchedule(classB.getId(), "Secret", 50000, "XOF", java.time.LocalDate.of(2026, 10, 1)), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "accountant-fees-a@ecole.example", Role.ACCOUNTANT);

        mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleB.getId() + "/generate-invoices")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    /**
     * Le comptable doit pouvoir répondre à « qui est en retard ? » sans parcourir les classes
     * une par une, et avec des noms d'élèves — l'écran affichait jusqu'ici des identifiants de
     * factures.
     */
    @Test
    void listsOverdueStudentsAcrossTheWholeSchoolWithTheirNames() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Retards");
        SchoolClass sixieme = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        SchoolClass cinquieme = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("5ème B", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("R1", "Awa", "Traoré", null, null, sixieme.getId()), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("R2", "Boubacar", "Diallo", null, null, cinquieme.getId()), tenant.getId()));

        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-late@ecole.example", Role.ACCOUNTANT);

        // Une échéance dépassée en 6ème, une échéance à venir en 5ème.
        Long overdueSchedule = createSchedule(token, sixieme.getId(), "Trimestre 1", 100000, LocalDate.now().minusDays(30));
        Long futureSchedule = createSchedule(token, cinquieme.getId(), "Trimestre 2", 80000, LocalDate.now().plusDays(30));
        generateInvoices(token, overdueSchedule);
        generateInvoices(token, futureSchedule);

        // Sans filtre : les deux impayés, toutes classes confondues.
        mockMvc.perform(get("/api/v1/school-fees/outstanding").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // En retard seulement : la 6ème, avec le nom de l'élève et l'ancienneté de la dette.
        mockMvc.perform(get("/api/v1/school-fees/outstanding")
                        .param("onlyOverdue", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].studentName").value("Awa Traoré"))
                .andExpect(jsonPath("$.data[0].className").value("6ème A"))
                .andExpect(jsonPath("$.data[0].scheduleLabel").value("Trimestre 1"))
                .andExpect(jsonPath("$.data[0].amountRemainingCents").value(100000))
                .andExpect(jsonPath("$.data[0].overdue").value(true))
                .andExpect(jsonPath("$.data[0].daysLate").value(30));
    }

    /** Une facture soldée sort de la liste de relance ; c'est ce qui rend la liste utilisable. */
    @Test
    void aSettledInvoiceLeavesTheOutstandingList() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Soldée");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("S1", "Fanta", "Keïta", null, null, schoolClass.getId()), tenant.getId()));

        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-settled@ecole.example", Role.ACCOUNTANT);
        Long scheduleId = createSchedule(token, schoolClass.getId(), "Trimestre 1", 50000, LocalDate.now().minusDays(10));
        Long invoiceId = generateInvoices(token, scheduleId);

        mockMvc.perform(get("/api/v1/school-fees/outstanding").param("onlyOverdue", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(1));

        // Un règlement partiel laisse la facture dans la liste, avec le reste à recouvrer.
        pay(token, invoiceId, 20000, "CASH").andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/school-fees/outstanding").param("onlyOverdue", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].amountRemainingCents").value(30000));

        pay(token, invoiceId, 30000, "MOBILE_MONEY").andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/school-fees/outstanding").param("onlyOverdue", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * Un trop-perçu gonflerait les encaissements et ferait passer le taux de recouvrement
     * au-dessus de 100 % sans qu'aucun écran ne le signale.
     */
    @Test
    void refusesAPaymentLargerThanTheRemainingBalance() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Trop-perçu");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("T1", "Moussa", "Sy", null, null, schoolClass.getId()), tenant.getId()));

        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-over@ecole.example", Role.ACCOUNTANT);
        Long scheduleId = createSchedule(token, schoolClass.getId(), "Trimestre 1", 50000, LocalDate.now());
        Long invoiceId = generateInvoices(token, scheduleId);

        pay(token, invoiceId, 60000, "CASH")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_EXCEEDS_BALANCE"));

        mockMvc.perform(get("/api/v1/school-fees/invoices/" + invoiceId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.amountPaidCents").value(0));
    }

    /** Les indicateurs du tableau de bord se calculent sur tout l'établissement. */
    @Test
    void summarisesCollectionAcrossTheWholeSchool() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Recouvrement");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("C1", "Aïcha", "Touré", null, null, schoolClass.getId()), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("C2", "Ibrahim", "Cissé", null, null, schoolClass.getId()), tenant.getId()));

        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-sum@ecole.example", Role.ACCOUNTANT);
        Long scheduleId = createSchedule(token, schoolClass.getId(), "Trimestre 1", 100000, LocalDate.now().minusDays(5));
        Long invoiceId = generateInvoices(token, scheduleId);
        pay(token, invoiceId, 100000, "CASH");

        mockMvc.perform(get("/api/v1/school-fees/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoicedCents").value(200000))
                .andExpect(jsonPath("$.data.collectedCents").value(100000))
                .andExpect(jsonPath("$.data.outstandingCents").value(100000))
                // Une seule facture reste due, et son échéance est passée.
                .andExpect(jsonPath("$.data.overdueCents").value(100000))
                .andExpect(jsonPath("$.data.overdueInvoiceCount").value(1))
                .andExpect(jsonPath("$.data.lateStudentCount").value(1))
                .andExpect(jsonPath("$.data.collectionRate").value(50.0))
                .andExpect(jsonPath("$.data.collectionByMethod[0].method").value("CASH"))
                .andExpect(jsonPath("$.data.collectionByMethod[0].amountCents").value(100000));

        // Le même bloc alimente le tableau de bord de la direction.
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-sum@ecole.example", Role.ADMIN);
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .param("from", LocalDate.now().minusMonths(1).toString())
                        .param("to", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finance.outstandingCents").value(100000))
                .andExpect(jsonPath("$.data.finance.lateStudentCount").value(1));
    }

    /** Le journal des encaissements sert au rapprochement de caisse : il nomme l'élève réglé. */
    @Test
    void listsPaymentsOfTheDayWithStudentNames() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Journal");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("J1", "Salif", "Koné", null, null, schoolClass.getId()), tenant.getId()));

        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-journal@ecole.example", Role.ACCOUNTANT);
        Long scheduleId = createSchedule(token, schoolClass.getId(), "Trimestre 1", 50000, LocalDate.now());
        Long invoiceId = generateInvoices(token, scheduleId);
        pay(token, invoiceId, 25000, "MOBILE_MONEY");

        mockMvc.perform(get("/api/v1/school-fees/payments")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].studentName").value("Salif Koné"))
                .andExpect(jsonPath("$.data[0].className").value("6ème A"))
                .andExpect(jsonPath("$.data[0].amountCents").value(25000))
                .andExpect(jsonPath("$.data[0].method").value("MOBILE_MONEY"));
    }

    private Long createSchedule(String token, Long schoolClassId, String label, long amountCents, LocalDate dueDate)
            throws Exception {
        String response = mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClassId + ",\"label\":\"" + label + "\",\"amountCents\":"
                                + amountCents + ",\"dueDate\":\"" + dueDate + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    /** Génère les factures et renvoie l'identifiant de la première. */
    private Long generateInvoices(String token, Long scheduleId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleId + "/generate-invoices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get(0).get("id").asLong();
    }

    private ResultActions pay(String token, Long invoiceId, long amountCents, String method) throws Exception {
        return mockMvc.perform(post("/api/v1/school-fees/invoices/" + invoiceId + "/payments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountCents\":" + amountCents + ",\"method\":\"" + method + "\"}"));
    }
}
