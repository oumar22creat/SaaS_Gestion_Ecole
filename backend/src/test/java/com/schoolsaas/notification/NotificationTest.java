package com.schoolsaas.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantSessionConfigurer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Registre centralisé de notifications (ROADMAP.md 2.5) : préférences, opt-out, isolation. */
class NotificationTest extends AbstractIntegrationTest {

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
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private TenantSessionConfigurer tenantSessionConfigurer;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void aUserSeesAllTypesEnabledByDefault() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Notifications");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-notif@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/notification-preferences").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(NotificationType.values().length))
                .andExpect(jsonPath("$.data[?(@.type=='NEW_MESSAGE')].enabled").value(true));
    }

    @Test
    void aUserCanDisableAType() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Notifications Off");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-notif-off@ecole.example", Role.ADMIN);

        mockMvc.perform(put("/api/v1/notification-preferences/NEW_MESSAGE")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/v1/notification-preferences").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='NEW_MESSAGE')].enabled").value(false))
                .andExpect(jsonPath("$.data[?(@.type=='ANNOUNCEMENT')].enabled").value(true));
    }

    @Test
    void dispatchSkipsRecipientsWhoOptedOut() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Notifications Dispatch");
        User user = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "teacher-notif@ecole.example", Role.TEACHER);
        preferenceRepository.save(
                TestAuthSupport.withTenant(new NotificationPreference(user.getId(), NotificationType.NEW_MESSAGE, false), tenant.getId()));

        notificationDispatcher.dispatch(NotificationType.NEW_MESSAGE, List.of(user.getId()), "Nouveau message", "corps");

        verify(notificationGateway, never()).send(argThat(event -> event.recipientUserIds().contains(user.getId())));
    }

    @Test
    void dispatchSendsToRecipientsWithoutAnExplicitPreference() {
        notificationDispatcher.dispatch(NotificationType.NEW_GRADE, List.of(999L), "Nouvelle note", "corps");

        verify(notificationGateway).send(argThat(event ->
                event.type() == NotificationType.NEW_GRADE && event.recipientUserIds().contains(999L)));
    }

    @Test
    @Transactional
    void hibernateFilterHidesNotificationPreferencesOfOtherTenants() {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Notif A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Notif B");
        User userB = TestAuthSupport.createUser(userRepository, passwordEncoder, tenantB, "admin-notif-b@ecole.example", Role.ADMIN);
        NotificationPreference preferenceB = preferenceRepository.save(
                TestAuthSupport.withTenant(new NotificationPreference(userB.getId(), NotificationType.ANNOUNCEMENT, false), tenantB.getId()));

        entityManager.flush();
        entityManager.clear();

        TenantContext.set(tenantA.getId());
        tenantSessionConfigurer.applyTenant(tenantA.getId());
        try {
            assertThat(preferenceRepository.findById(preferenceB.getId())).isEmpty();
            assertThat(preferenceRepository.findAll())
                    .extracting(NotificationPreference::getId)
                    .doesNotContain(preferenceB.getId());
        } finally {
            TenantContext.clear();
        }
    }
}
