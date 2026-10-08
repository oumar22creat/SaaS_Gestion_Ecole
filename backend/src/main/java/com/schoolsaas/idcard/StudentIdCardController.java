package com.schoolsaas.idcard;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassService;
import com.schoolsaas.schoolyear.SchoolYear;
import com.schoolsaas.schoolyear.SchoolYearService;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentPhotoService;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.student.StudentService;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantLogoService;
import com.schoolsaas.tenant.TenantRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cartes d'identité scolaires — une par élève, ou toute une classe en une planche.
 *
 * <p>La carte sert à l'entrée de l'établissement, aux examens et aux sorties scolaires.
 * Elle n'est donc pas un gadget : c'est un document que l'élève porte sur lui, et que le
 * secrétariat doit pouvoir produire pour cinquante élèves d'un seul geste à la rentrée.
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
public class StudentIdCardController {

    private final StudentIdCardPdfExporter exporteur;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final StudentPhotoService photoService;
    private final SchoolClassService schoolClassService;
    private final SchoolYearService schoolYearService;
    private final TenantRepository tenantRepository;
    private final TenantLogoService tenantLogoService;

    public StudentIdCardController(
            StudentIdCardPdfExporter exporteur,
            StudentService studentService,
            StudentRepository studentRepository,
            StudentPhotoService photoService,
            SchoolClassService schoolClassService,
            SchoolYearService schoolYearService,
            TenantRepository tenantRepository,
            TenantLogoService tenantLogoService) {
        this.exporteur = exporteur;
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.photoService = photoService;
        this.schoolClassService = schoolClassService;
        this.schoolYearService = schoolYearService;
        this.tenantRepository = tenantRepository;
        this.tenantLogoService = tenantLogoService;
    }

    @GetMapping("/students/{studentId}/id-card.pdf")
    public ResponseEntity<byte[]> carteEleve(@PathVariable Long studentId) {
        Student eleve = studentService.getById(studentId);
        if (eleve.getSchoolClassId() == null) {
            throw ApiException.unprocessable(
                    "STUDENT_WITHOUT_CLASS", "Cet élève n'est affecté à aucune classe : sa carte n'aurait pas de niveau");
        }
        return reponse(
                exporteur.export(List.of(eleve), schoolClassService.getById(eleve.getSchoolClassId()),
                        photoService.contenus(List.of(eleve)), anneeScolaire(), tenant(), logo()),
                "carte-" + assainir(eleve.getLastName() + "-" + eleve.getFirstName()) + ".pdf");
    }

    /**
     * Toute la classe en une planche, huit cartes par page A4, en ordre alphabétique.
     *
     * <p>Les élèves désactivés sont exclus : imprimer la carte d'un enfant qui a quitté
     * l'établissement, c'est lui fournir un titre d'accès qu'il ne devrait plus avoir.
     */
    @GetMapping("/classes/{schoolClassId}/id-cards.pdf")
    public ResponseEntity<byte[]> cartesDeLaClasse(@PathVariable Long schoolClassId) {
        SchoolClass classe = schoolClassService.getById(schoolClassId);
        List<Student> eleves = studentRepository.findAllBySchoolClassId(schoolClassId).stream()
                .filter(Student::isActive)
                .sorted(Comparator
                        .comparing(Student::getLastName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Student::getFirstName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<Long, byte[]> portraits = photoService.contenus(eleves);
        return reponse(
                exporteur.export(eleves, classe, portraits, anneeScolaire(), tenant(), logo()),
                "cartes-" + assainir(classe.getName()) + ".pdf");
    }

    /**
     * Libellé de l'année active, ou les deux années civiles à cheval si aucune n'est ouverte :
     * une carte sans année ne dit pas jusqu'à quand elle vaut.
     */
    private String anneeScolaire() {
        return schoolYearService.active().map(SchoolYear::getLabel).orElseGet(() -> {
            int annee = java.time.LocalDate.now().getMonthValue() >= 8
                    ? java.time.LocalDate.now().getYear()
                    : java.time.LocalDate.now().getYear() - 1;
            return annee + "-" + (annee + 1);
        });
    }

    private Tenant tenant() {
        return tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
    }

    private byte[] logo() {
        return tenantLogoService.content(tenant()).orElse(null);
    }

    private ResponseEntity<byte[]> reponse(byte[] pdf, String nomFichier) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomFichier).build().toString())
                .body(pdf);
    }

    private static String assainir(String valeur) {
        return java.text.Normalizer.normalize(valeur, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
