package com.schoolsaas.tenant;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Logo de l'établissement sur les documents officiels (cahier-des-charges.md §2.4). Le logo
 * est téléversé et non récupéré depuis l'URL des réglages : les PDF sont fabriqués par le
 * serveur, et suivre une adresse saisie par un administrateur en ferait un relais vers
 * n'importe quelle machine du réseau interne.
 */
class TenantLogoTest extends AbstractIntegrationTest {

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
    void uploadsALogoAndServesItBack() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Logo");
        String token = adminToken(tenant, "admin-logo@ecole.example");

        mockMvc.perform(multipart("/api/v1/tenants/current/logo")
                        .file(pngFile())
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk());

        // Servi publiquement : le logo s'affiche sur l'écran de connexion, avant toute
        // authentification.
        byte[] served = mockMvc.perform(get("/api/v1/tenants/current/logo")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(served).isNotEmpty();
    }

    /**
     * Le type déclaré par le navigateur ne prouve rien. Sans vérification de la signature, un
     * fichier quelconque renommé en .png ferait échouer la génération de TOUS les bulletins,
     * longtemps après le téléversement — et personne ne ferait le lien.
     */
    @Test
    void refusesAFileThatIsNotReallyAnImage() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Faux Logo");
        String token = adminToken(tenant, "admin-faux@ecole.example");

        mockMvc.perform(multipart("/api/v1/tenants/current/logo")
                        .file(new MockMultipartFile("file", "logo.png", "image/png", "ceci n'est pas une image".getBytes()))
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_LOGO_FORMAT"));
    }

    /** Un enseignant ne change pas l'identité visuelle de l'établissement. */
    @Test
    void aTeacherCannotChangeTheLogo() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Droits Logo");
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-logo@ecole.example", Role.TEACHER);

        mockMvc.perform(multipart("/api/v1/tenants/current/logo")
                        .file(pngFile())
                        .header("Authorization", "Bearer " + teacherToken)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isForbidden());
    }

    /**
     * Le logo doit apparaître sur les documents officiels : c'est tout l'objet du
     * téléversement. Un PDF plus lourd qu'un PDF sans logo prouve que l'image y est
     * réellement intégrée, et non simplement référencée.
     */
    @Test
    void theLogoEndsUpInsideTheOfficialDocuments() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Certif Logo");
        String token = adminToken(tenant, "admin-certif-logo@ecole.example");
        SchoolClass schoolClass = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(
                new Student("L-1", "Awa", "Traoré", LocalDate.of(2012, 1, 2), "F", schoolClass.getId()), tenant.getId()));
        activateYear(token);

        byte[] withoutLogo = certificate(token, tenant, student);

        mockMvc.perform(multipart("/api/v1/tenants/current/logo")
                        .file(pngFile())
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk());

        byte[] withLogo = certificate(token, tenant, student);
        assertThat(withLogo.length).isGreaterThan(withoutLogo.length);
    }

    private byte[] certificate(String token, Tenant tenant, Student student) throws Exception {
        return mockMvc.perform(get("/api/v1/paperwork/students/" + student.getId() + "/enrollment-certificate.pdf")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private MockMultipartFile pngFile() throws Exception {
        BufferedImage image = new BufferedImage(120, 60, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0, 0, 120, 60);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", "logo.png", "image/png", out.toByteArray());
    }

    private String adminToken(Tenant tenant, String email) throws Exception {
        return TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, email, Role.ADMIN);
    }

    private void activateYear(String token) throws Exception {
        String response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/school-years")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"2026-2027\",\"startDate\":\"2026-10-01\",\"endDate\":\"2027-07-31\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long yearId = objectMapper.readTree(response).get("data").get("id").asLong();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/school-years/" + yearId + "/activate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
