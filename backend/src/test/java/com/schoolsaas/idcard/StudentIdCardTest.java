package com.schoolsaas.idcard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Photo de l'élève et cartes d'identité scolaires, à l'unité et par classe. */
class StudentIdCardTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private StudentRepository studentRepository;

    /**
     * Une classe entière en une planche. Le cas qui compte est celui où tous les portraits
     * ne sont pas là : le secrétariat ne peut pas attendre les cinquante photos avant
     * d'imprimer la première carte.
     */
    @Test
    void aWholeClassPrintsAsOneSheetEvenWhenSomePhotosAreMissing() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Cartes");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-cartes@ecole.example", Role.ADMIN);
        SchoolClass classe = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème année A", null), tenant.getId()));

        Student avecPhoto = studentRepository.save(TestAuthSupport.withTenant(
                new Student("CI-001", "Awa", "Ba", LocalDate.of(2014, 3, 12), "F", classe.getId()), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(
                new Student("CI-002", "Moussa", "Zongo", LocalDate.of(2014, 7, 4), "M", classe.getId()), tenant.getId()));
        Student parti = studentRepository.save(TestAuthSupport.withTenant(
                new Student("CI-003", "Parti", "Ailleurs", null, "M", classe.getId()), tenant.getId()));
        parti.setActive(false);
        studentRepository.save(parti);

        mockMvc.perform(multipart("/api/v1/students/" + avecPhoto.getId() + "/photo")
                        .file(new MockMultipartFile("file", "awa.png", "image/png", portraitPng()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/students/" + avecPhoto.getId() + "/photo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        byte[] pdf = mockMvc.perform(get("/api/v1/classes/" + classe.getId() + "/id-cards.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            // Deux élèves actifs : une seule planche, huit emplacements par page.
            org.assertj.core.api.Assertions.assertThat(document.getNumberOfPages()).isEqualTo(1);
            String texte = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            org.assertj.core.api.Assertions.assertThat(texte)
                    .contains("CARTE D'ÉLÈVE")
                    .contains("CI-001")
                    .contains("ZONGO")
                    .contains("6ème année A")
                    // Le portrait manquant est signalé sur la carte, pas passé sous silence.
                    .contains("MANQUANTE")
                    // L'élève désactivé n'a pas de carte : ce serait un titre d'accès indu.
                    .doesNotContain("CI-003");
        }
    }

    /** Un fichier renommé en .png n'est pas une image : le refuser au dépôt, pas à l'impression. */
    @Test
    void aFileThatIsNotAnImageIsRejectedOnUploadRatherThanBreakingThePrintRun() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Photo Refus");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-refus@ecole.example", Role.ADMIN);
        SchoolClass classe = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("5ème année A", null), tenant.getId()));
        Student eleve = studentRepository.save(TestAuthSupport.withTenant(
                new Student("CI-010", "Fanta", "Sow", null, "F", classe.getId()), tenant.getId()));

        mockMvc.perform(multipart("/api/v1/students/" + eleve.getId() + "/photo")
                        .file(new MockMultipartFile("file", "faux.png", "image/png", "ceci n'est pas une image".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_PHOTO_FORMAT"));
    }

    /** Une classe vide doit le dire plutôt que rendre un PDF de zéro page. */
    @Test
    void printingTheCardsOfAnEmptyClassIsRefusedWithAClearMessage() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Cartes Vide");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-cvide@ecole.example", Role.ADMIN);
        SchoolClass classe = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("4ème année A", null), tenant.getId()));

        mockMvc.perform(get("/api/v1/classes/" + classe.getId() + "/id-cards.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_STUDENT_TO_PRINT"));
    }

    private static byte[] portraitPng() throws Exception {
        BufferedImage image = new BufferedImage(240, 320, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xD0, 0xE6, 0xDE));
        g.fillRect(0, 0, 240, 320);
        g.setColor(new Color(0x0f, 0x5c, 0x4c));
        g.fillOval(70, 60, 100, 100);
        g.fillOval(40, 180, 160, 160);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
