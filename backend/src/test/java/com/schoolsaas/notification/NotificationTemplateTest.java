package com.schoolsaas.notification;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.library.Book;
import com.schoolsaas.library.BookLoan;
import com.schoolsaas.library.BookLoanRepository;
import com.schoolsaas.library.BookRepository;
import com.schoolsaas.library.LibraryOverdueReminderJob;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Templates de notification/e-mail personnalisables par établissement (ROADMAP.md 3.7,
 * dernier item) : RBAC, validation du placeholder, et effet réel sur un envoi via
 * {@link NotificationDispatcher} (vérifié à travers {@link LibraryOverdueReminderJob}, déjà
 * couvert sans template par LibraryTest).
 */
class NotificationTemplateTest extends AbstractIntegrationTest {

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

    @Test
    void listsAllTypesWithNoOverrideByDefaultAndRestrictsWritesToAdminOrDirection() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Templates");
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-templates@ecole.example", Role.ADMIN);
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretary-templates@ecole.example", Role.SECRETARY);

        mockMvc.perform(get("/api/v1/tenants/current/notification-templates").header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/tenants/current/notification-templates").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(NotificationType.values().length));

        mockMvc.perform(put("/api/v1/tenants/current/notification-templates/ABSENCE")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleOverride\":\"Alerte\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsABodyTemplateMissingTheMessagePlaceholder() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Templates Invalides");
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-invalid-template@ecole.example", Role.ADMIN);

        mockMvc.perform(put("/api/v1/tenants/current/notification-templates/LIBRARY_OVERDUE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bodyTemplate\":\"Merci de régulariser rapidement.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_NOTIFICATION_TEMPLATE"));
    }

    @Test
    void anOverdueReminderUsesTheTenantsCustomTemplateOnceConfigured() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Templates Bibliothèque");
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-template-lib@ecole.example", Role.ADMIN);

        mockMvc.perform(put("/api/v1/tenants/current/notification-templates/LIBRARY_OVERDUE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleOverride\":\"Collège Voltaire — Retard\",\"bodyTemplate\":\"[Collège Voltaire] {message} Merci de régulariser.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titleOverride").value("Collège Voltaire — Retard"));

        Student student = studentRepository.save(TestAuthSupport.withTenant(new Student("LT1", "Rosalind", "F", null, null, null), tenant.getId()));
        Book book = bookRepository.save(TestAuthSupport.withTenant(new Book("BC-TEMPLATE", null, "Retard Templaté", "Auteur", 1), tenant.getId()));
        bookLoanRepository.save(TestAuthSupport.withTenant(
                new BookLoan(book.getId(), student.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)), tenant.getId()));

        libraryOverdueReminderJob.remindOverdueLoansFor(tenant.getId(), LocalDate.of(2026, 9, 20));

        verify(notificationGateway).send(argThat(event ->
                event.type() == NotificationType.LIBRARY_OVERDUE
                        && event.title().equals("Collège Voltaire — Retard")
                        && event.body().startsWith("[Collège Voltaire] Élève " + student.getId() + " —")
                        && event.body().endsWith("Merci de régulariser.")));
    }
}
