package com.schoolsaas.paperwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Documents officiels du guichet (cahier-des-charges.md §7/§19.4) : ce que le secrétariat
 * imprime tous les jours et que l'application ne savait pas produire.
 */
class PaperworkTest extends AbstractIntegrationTest {

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

    @Test
    void issuesAnEnrollmentCertificateForAnActiveStudent() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Certificats");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sec-cert@ecole.example", Role.SECRETARY);
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-cert@ecole.example", Role.ADMIN);

        SchoolClass schoolClass = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(
                new Student("C-1", "Awa", "Traoré", LocalDate.of(2012, 1, 2), "F", schoolClass.getId()), tenant.getId()));

        activateYear(adminToken);

        byte[] pdf = mockMvc.perform(get("/api/v1/paperwork/students/" + student.getId() + "/enrollment-certificate.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // Un PDF valide commence par %PDF ; le contenu détaillé relève de la mise en page.
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(500);
    }

    /**
     * Un certificat atteste une situation présente. Le délivrer pour un élève désactivé — donc
     * parti — serait une fausse attestation signée par l'établissement.
     */
    @Test
    void refusesACertificateForAnInactiveStudent() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Certif Inactif");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sec-inactif@ecole.example", Role.SECRETARY);
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-inactif@ecole.example", Role.ADMIN);

        SchoolClass schoolClass = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(
                new Student("C-2", "Moussa", "Sy", null, null, schoolClass.getId()), tenant.getId()));
        student.setActive(false);
        studentRepository.save(student);

        activateYear(adminToken);

        mockMvc.perform(get("/api/v1/paperwork/students/" + student.getId() + "/enrollment-certificate.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    /** Le reçu est la copie de l'écriture comptable : il se construit depuis le règlement enregistré. */
    @Test
    void issuesAReceiptForARecordedPayment() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Reçus");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "compta-recu@ecole.example", Role.ACCOUNTANT);

        SchoolClass schoolClass = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(
                new Student("R-1", "Fanta", "Keïta", null, null, schoolClass.getId()), tenant.getId()));

        String scheduleResponse = mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"label\":\"Trimestre 1\","
                                + "\"amountCents\":50000,\"dueDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long scheduleId = objectMapper.readTree(scheduleResponse).get("data").get("id").asLong();

        String invoiceResponse = mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleId + "/generate-invoices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long invoiceId = objectMapper.readTree(invoiceResponse).get("data").get(0).get("id").asLong();

        String paymentResponse = mockMvc.perform(post("/api/v1/school-fees/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":20000,\"method\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long paymentId = objectMapper.readTree(paymentResponse).get("data").get("id").asLong();

        byte[] pdf = mockMvc.perform(get("/api/v1/paperwork/payments/" + paymentId + "/receipt.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    /** Un enseignant n'a pas à délivrer de documents officiels au nom de l'établissement. */
    @Test
    void aTeacherCannotIssueOfficialDocuments() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Droits Papiers");
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-papiers@ecole.example", Role.TEACHER);

        mockMvc.perform(get("/api/v1/paperwork/students/1/enrollment-certificate.pdf")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/paperwork/payments/1/receipt.pdf")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    private void activateYear(String adminToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/school-years")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"2026-2027\",\"startDate\":\"2026-10-01\",\"endDate\":\"2027-07-31\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long yearId = objectMapper.readTree(response).get("data").get("id").asLong();
        mockMvc.perform(put("/api/v1/school-years/" + yearId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /**
     * La fiche d'inscription, telle que le guichet l'imprime et la fait signer.
     *
     * <p>Deux exigences qui ne vont pas de soi : elle doit porter le détail des frais de la
     * classe avec leur total — c'est la seule trace écrite de ce que la famille s'engage à
     * payer — et elle doit sortir même pour un élève sans photo ni responsable enregistré,
     * puisque c'est précisément le cas d'une inscription qu'on est en train de faire.
     */
    @Test
    void theEnrollmentFormCarriesTheFeesAndPrintsEvenForAStudentWithNoParentOnFile() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Fiche");
        tenant.setDirectorName("Mme Fatoumata Sidibé");
        tenant.setOfficialAuthority("RÉPUBLIQUE DU MALI");
        tenantRepository.save(tenant);
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretariat-fiche@ecole.example",
                Role.SECRETARY);
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-fiche@ecole.example",
                Role.ADMIN);
        activateYear(adminToken);

        SchoolClass classe = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème année A", null), tenant.getId()));
        Student eleve = studentRepository.save(TestAuthSupport.withTenant(
                new Student("FI-001", "Fanta", "Diallo", LocalDate.of(2014, 3, 12), "F", classe.getId()),
                tenant.getId()));

        mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + classe.getId() + ",\"label\":\"Frais d'inscription\","
                                + "\"amountCents\":30000,\"dueDate\":\"2026-10-15\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + classe.getId() + ",\"label\":\"Scolarité 1er trimestre\","
                                + "\"amountCents\":60000,\"dueDate\":\"2026-11-30\"}"))
                .andExpect(status().isCreated());

        byte[] pdf = mockMvc.perform(get("/api/v1/paperwork/students/" + eleve.getId() + "/enrollment-form.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String texte = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertThat(texte)
                    .contains("FICHE D'INSCRIPTION")
                    .contains("RÉPUBLIQUE DU MALI")
                    .contains("Diallo")
                    .contains("FI-001")
                    .contains("6ème année A")
                    .contains("Féminin")
                    // Les lignes à remplir à la main doivent exister, sinon la fiche est refaite.
                    .contains("Lieu de naissance")
                    .contains("RESPONSABLES LÉGAUX")
                    .contains("Frais d'inscription")
                    .contains("Scolarité 1er trimestre")
                    // 30 000 + 60 000 : l'engagement financier total, écrit noir sur blanc.
                    .contains("TOTAL DÛ POUR L'ANNÉE")
                    .contains("90 000 XOF")
                    .contains("Le responsable légal")
                    .contains("Mme Fatoumata Sidibé");
        }
    }

    /** Un enseignant n'inscrit pas : la fiche engage l'établissement et la famille. */
    @Test
    void aTeacherCannotPrintAnEnrollmentForm() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Fiche Rôle");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-fiche@ecole.example",
                Role.TEACHER);
        SchoolClass classe = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("5ème année A", null), tenant.getId()));
        Student eleve = studentRepository.save(TestAuthSupport.withTenant(
                new Student("FI-900", "Moussa", "Traoré", null, "M", classe.getId()), tenant.getId()));

        mockMvc.perform(get("/api/v1/paperwork/students/" + eleve.getId() + "/enrollment-form.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
