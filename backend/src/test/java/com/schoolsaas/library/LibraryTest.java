package com.schoolsaas.library;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.SubscriptionRepository;
import com.schoolsaas.notification.NotificationGateway;
import com.schoolsaas.notification.NotificationType;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Bibliothèque (ROADMAP.md 3.5) : catalogue, emprunts/retours, réservations/liste d'attente, relances, RBAC, isolation. */
class LibraryTest extends AbstractIntegrationTest {

    @MockBean
    private NotificationGateway notificationGateway;

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
    private StudentRepository studentRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookLoanRepository bookLoanRepository;

    @Autowired
    private LibraryOverdueReminderJob libraryOverdueReminderJob;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Test
    void lendsAndReturnsABookReleasingTheWaitingListAutomatically() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Bibliothèque");
        TestAuthSupport.grantAllPlanFeatures(subscriptionRepository, planRepository, tenant);
        Student borrower = studentRepository.save(TestAuthSupport.withTenant(new Student("L1", "Ada", "L", null, null, null), tenant.getId()));
        Student waitingStudent = studentRepository.save(TestAuthSupport.withTenant(new Student("L2", "Alan", "T", null, null, null), tenant.getId()));
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretary-library@ecole.example", Role.SECRETARY);
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-library@ecole.example", Role.TEACHER);

        mockMvc.perform(post("/api/v1/library/books")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"BC-1\",\"isbn\":\"978-1\",\"title\":\"Les Misérables\",\"author\":\"Hugo\",\"totalCopies\":1}"))
                .andExpect(status().isForbidden());

        String bookResp = mockMvc.perform(post("/api/v1/library/books")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"BC-1\",\"isbn\":\"978-1\",\"title\":\"Les Misérables\",\"author\":\"Hugo\",\"totalCopies\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.availableCopies").value(1))
                .andReturn().getResponse().getContentAsString();
        Long bookId = objectMapper.readTree(bookResp).get("data").get("id").asLong();

        String loanResp = mockMvc.perform(post("/api/v1/library/books/" + bookId + "/loans")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + borrower.getId() + ",\"dueDate\":\"2026-10-15\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long loanId = objectMapper.readTree(loanResp).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/library/books/" + bookId).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableCopies").value(0));

        mockMvc.perform(post("/api/v1/library/books/" + bookId + "/loans")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + waitingStudent.getId() + ",\"dueDate\":\"2026-10-15\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NO_COPIES_AVAILABLE"));

        mockMvc.perform(post("/api/v1/library/books/" + bookId + "/reservations")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + waitingStudent.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.position").value(1));

        mockMvc.perform(post("/api/v1/library/loans/" + loanId + "/return").header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnedAt").exists());

        mockMvc.perform(get("/api/v1/library/books/" + bookId + "/reservations").header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void theOverdueReminderJobLogsANotificationForEachLateLoan() {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Bibliothèque Retard");
        Student student = studentRepository.save(TestAuthSupport.withTenant(new Student("L3", "Grace", "H", null, null, null), tenant.getId()));
        Book book = bookRepository.save(TestAuthSupport.withTenant(new Book("BC-OVERDUE", null, "Retard", "Auteur", 1), tenant.getId()));
        bookLoanRepository.save(TestAuthSupport.withTenant(
                new BookLoan(book.getId(), student.getId(), java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 15)),
                tenant.getId()));

        // Cible directement CE tenant plutôt que runFor() (qui parcourt tous les tenants de la
        // suite de tests, partagés via le conteneur Testcontainers "singleton" — voir
        // AbstractIntegrationTest) : évite toute pollution par des données d'autres classes de
        // test et rend l'assertion déterministe.
        libraryOverdueReminderJob.remindOverdueLoansFor(tenant.getId(), java.time.LocalDate.of(2026, 9, 20));

        verify(notificationGateway).send(argThat(event ->
                event.type() == NotificationType.LIBRARY_OVERDUE
                        && event.body().contains("Élève " + student.getId() + " —")));
    }

    @Test
    void aTenantCanNeverSeeBooksOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Bibliothèque A");
        TestAuthSupport.grantAllPlanFeatures(subscriptionRepository, planRepository, tenantA);
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Bibliothèque B");
        Book bookB = bookRepository.save(TestAuthSupport.withTenant(new Book("BC-SECRET", null, "Secret", "Auteur", 1), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "secretary-library-a@ecole.example", Role.SECRETARY);

        mockMvc.perform(get("/api/v1/library/books/" + bookB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
