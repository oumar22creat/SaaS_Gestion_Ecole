package com.schoolsaas.schoolyear;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolyear.dto.EnrollmentResponse;
import com.schoolsaas.schoolyear.dto.PromotionRequest;
import com.schoolsaas.schoolyear.dto.PromotionResult;
import com.schoolsaas.schoolyear.dto.SchoolYearRequest;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Années scolaires et inscriptions (cahier-des-charges.md §7).
 *
 * <p>Deux invariants portent tout le reste :
 * <ul>
 *   <li>une seule année ACTIVE par établissement — sinon « l'année en cours » devient ambigu
 *       et chaque écran choisit la sienne ;</li>
 *   <li>une seule inscription par élève et par année — c'est ce qui rend la rentrée
 *       rejouable sans créer de doublons.</li>
 * </ul>
 * Le second est garanti par un index unique en base, pas seulement ici : une rentrée lancée
 * deux fois depuis deux onglets ne doit pas dépendre du hasard.
 */
@Service
public class SchoolYearService {

    private final SchoolYearRepository schoolYearRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;

    public SchoolYearService(
            SchoolYearRepository schoolYearRepository,
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository) {
        this.schoolYearRepository = schoolYearRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    // ----------------------------------------------------------------------- Années

    public List<SchoolYear> list() {
        return schoolYearRepository.findAllByOrderByStartDateDesc();
    }

    public SchoolYear getById(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_YEAR_NOT_FOUND", "Année scolaire introuvable"));
    }

    /** L'année sur laquelle travaille le produit, ou vide tant qu'aucune n'est activée. */
    public java.util.Optional<SchoolYear> active() {
        return schoolYearRepository.findByStatus(SchoolYearStatus.ACTIVE);
    }

    public SchoolYear requireActive() {
        return active().orElseThrow(() -> ApiException.unprocessable(
                "NO_ACTIVE_SCHOOL_YEAR", "Aucune année scolaire active : créez-en une et activez-la"));
    }

    public long enrolledCount(Long schoolYearId) {
        return enrollmentRepository.countBySchoolYearId(schoolYearId);
    }

    @Transactional
    public SchoolYear create(SchoolYearRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw ApiException.unprocessable("INVALID_SCHOOL_YEAR_DATES", "La date de fin doit suivre la date de début");
        }
        if (schoolYearRepository.findByLabel(request.label()).isPresent()) {
            throw ApiException.conflict("SCHOOL_YEAR_ALREADY_EXISTS", "Cette année scolaire existe déjà");
        }
        // Créée en PLANNED : on prépare la rentrée sans interrompre l'année en cours.
        return schoolYearRepository.save(new SchoolYear(
                request.label(), request.startDate(), request.endDate(), SchoolYearStatus.PLANNED));
    }

    /**
     * Bascule l'établissement sur cette année. L'année active précédente est clôturée dans la
     * même transaction : l'index unique en base refuse deux années actives, et le faire en
     * deux opérations laisserait une fenêtre où l'établissement n'en a aucune.
     */
    @Transactional
    public SchoolYear activate(Long id) {
        SchoolYear target = getById(id);
        if (target.getStatus() == SchoolYearStatus.CLOSED) {
            throw ApiException.unprocessable("SCHOOL_YEAR_CLOSED", "Une année clôturée ne peut pas être réactivée");
        }
        active().filter(current -> !current.getId().equals(id)).ifPresent(current -> {
            current.setStatus(SchoolYearStatus.CLOSED);
            schoolYearRepository.saveAndFlush(current);
        });
        target.setStatus(SchoolYearStatus.ACTIVE);
        return schoolYearRepository.save(target);
    }

    @Transactional
    public SchoolYear close(Long id) {
        SchoolYear year = getById(id);
        year.setStatus(SchoolYearStatus.CLOSED);
        return schoolYearRepository.save(year);
    }

    // ------------------------------------------------------------------ Inscriptions

    /**
     * Inscrit un élève sur l'année active et met à jour sa classe courante. Les deux écritures
     * vont ensemble : tout le produit (appel, notes, frais) lit {@code students.school_class_id},
     * l'historique vit dans {@code enrollments}.
     */
    @Transactional
    public Enrollment enroll(Long studentId, Long schoolClassId) {
        return enrollInto(requireActive(), studentId, schoolClassId);
    }

    @Transactional
    public Enrollment enrollInto(SchoolYear year, Long studentId, Long schoolClassId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
        if (schoolClassRepository.findById(schoolClassId).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }

        Enrollment enrollment = enrollmentRepository.findBySchoolYearIdAndStudentId(year.getId(), studentId)
                .map(existing -> {
                    // Réinscrire sur la même année est une correction de classe, pas un doublon.
                    existing.setSchoolClassId(schoolClassId);
                    existing.setStatus(EnrollmentStatus.ENROLLED);
                    return existing;
                })
                .orElseGet(() -> new Enrollment(year.getId(), studentId, schoolClassId, year.getStartDate()));
        Enrollment saved = enrollmentRepository.save(enrollment);

        if (year.getStatus() == SchoolYearStatus.ACTIVE) {
            student.setSchoolClassId(schoolClassId);
            studentRepository.save(student);
        }
        return saved;
    }

    public List<Enrollment> enrollmentsForYear(Long schoolYearId) {
        return enrollmentRepository.findAllBySchoolYearId(schoolYearId);
    }

    /** Parcours d'un élève, année la plus récente d'abord. */
    public List<EnrollmentResponse> historyForStudent(Long studentId) {
        Map<Long, SchoolYear> yearsById = schoolYearRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolYear::getId, Function.identity()));
        Map<Long, String> classNames = classNames();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));

        return enrollmentRepository.findAllByStudentId(studentId).stream()
                .sorted(Comparator.comparing(
                        (Enrollment e) -> yearsById.get(e.getSchoolYearId()).getStartDate()).reversed())
                .map(enrollment -> EnrollmentResponse.from(
                        enrollment,
                        yearsById.get(enrollment.getSchoolYearId()).getLabel(),
                        student.getFirstName() + " " + student.getLastName(),
                        student.getStudentNumber(),
                        classNames.getOrDefault(enrollment.getSchoolClassId(), "—")))
                .toList();
    }

    // --------------------------------------------------------------------- Rentrée

    /**
     * Réinscrit une promotion entière sur l'année cible, selon la décision prise pour chaque
     * élève. Ce qui n'est pas réinscrit est rapporté dans {@code skipped} plutôt que
     * silencieusement ignoré : à la rentrée, un élève oublié est un élève perdu.
     */
    @Transactional
    public PromotionResult promote(Long targetYearId, PromotionRequest request) {
        SchoolYear target = getById(targetYearId);
        SchoolYear source = getById(request.sourceYearId());
        if (target.getId().equals(source.getId())) {
            throw ApiException.unprocessable(
                    "SAME_SCHOOL_YEAR", "L'année de départ et l'année d'arrivée doivent être différentes");
        }

        Map<Long, Enrollment> sourceEnrollments = enrollmentRepository.findAllBySchoolYearId(source.getId()).stream()
                .collect(Collectors.toMap(Enrollment::getStudentId, Function.identity(), (a, b) -> a));
        Map<Long, String> classNames = classNames();

        int promoted = 0;
        int repeating = 0;
        int left = 0;
        List<String> skipped = new ArrayList<>();

        for (PromotionRequest.Decision decision : request.decisions()) {
            Enrollment sourceEnrollment = sourceEnrollments.get(decision.studentId());
            if (sourceEnrollment == null) {
                skipped.add("Élève #" + decision.studentId() + " : non inscrit sur l'année de départ");
                continue;
            }

            sourceEnrollment.setStatus(decision.outcome());
            switch (decision.outcome()) {
                case PROMOTED -> {
                    if (decision.targetClassId() == null) {
                        skipped.add("Élève #" + decision.studentId() + " : aucune classe d'arrivée choisie");
                        continue;
                    }
                    enrollInto(target, decision.studentId(), decision.targetClassId());
                    promoted++;
                }
                case REPEATING -> {
                    enrollInto(target, decision.studentId(), sourceEnrollment.getSchoolClassId());
                    repeating++;
                }
                case TRANSFERRED, GRADUATED, WITHDRAWN -> {
                    sourceEnrollment.setLeftAt(source.getEndDate());
                    left++;
                }
                case ENROLLED -> skipped.add(
                        "Élève #" + decision.studentId() + " : « inscrit » n'est pas une décision de fin d'année");
            }
            enrollmentRepository.save(sourceEnrollment);
        }

        return new PromotionResult(target.getId(), target.getLabel(), promoted, repeating, left, skipped);
    }

    private Map<Long, String> classNames() {
        return schoolClassRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
    }
}
