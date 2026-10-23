package com.schoolsaas.teacher;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.auth.Role;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.EmailNormalizer;
import com.schoolsaas.schoolclass.ClassSubjectAssignment;
import com.schoolsaas.schoolclass.ClassSubjectAssignmentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.timetable.TimetableEntry;
import com.schoolsaas.timetable.TimetableEntryRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Périmètre d'un enseignant : les classes et les couples (classe, matière) qui lui sont
 * affectés. Point de contrôle unique pour les modules qu'un TEACHER utilise (appel, notes,
 * bulletins, emploi du temps, cahier de textes) — voir docs/ARCHITECTURE.md ADR-030.
 *
 * <p>Le {@code @PreAuthorize} des contrôleurs vérifie le rôle ; il ne dit pas QUELLES classes.
 * Sans ce contrôle, un enseignant lisait et saisissait l'appel ou les notes de n'importe
 * quelle classe de son établissement en changeant un identifiant dans l'URL.
 *
 * <p>Seul le rôle TEACHER est restreint. ADMIN, DIRECTION, VIE_SCOLAIRE et les autres rôles
 * déjà autorisés par {@code @PreAuthorize} gardent un périmètre établissement entier : toutes
 * les méthodes {@code require*} sont sans effet pour eux. Le cloisonnement entre
 * établissements reste assuré en amont par le filtre Hibernate et la RLS (ADR-001).
 *
 * <p><b>Rattachement compte / fiche</b> : un compte {@code users} de rôle TEACHER n'a pas de
 * clé vers la fiche {@code teachers} (ADR-010). Le lien se fait par l'adresse e-mail, comparée
 * sous forme canonique des deux côtés (trim + minuscules, {@link EmailNormalizer}), sur les
 * fiches ACTIVES de l'établissement courant. Aucune fiche, ou plusieurs fiches actives pour la
 * même adresse : périmètre vide, le compte est refusé partout (fail-closed).
 *
 * <p><b>Sources d'affectation</b> (union) :
 * <ul>
 *   <li>{@code class_subject_assignments} — l'affectation officielle (classe, matière) ;</li>
 *   <li>{@code timetable_entries} — un créneau (classe, matière, enseignant) vaut affectation :
 *       le guide de configuration mène à l'emploi du temps sans passer par l'écran
 *       d'affectation, beaucoup d'établissements n'ont que celui-là ;</li>
 *   <li>{@code school_classes.head_teacher_id} — le professeur principal accède à SA classe
 *       (appel, bulletins, cahier de textes en lecture), mais pas aux notes des autres matières.</li>
 * </ul>
 */
@Component
public class TeacherScope {

    public static final String DENIED_CODE = "OUT_OF_TEACHER_SCOPE";

    private static final String REQUEST_ATTRIBUTE = TeacherScope.class.getName() + ".perimeter";

    private static final Logger log = LoggerFactory.getLogger(TeacherScope.class);

    private final TeacherRepository teacherRepository;
    private final ClassSubjectAssignmentRepository assignmentRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    public TeacherScope(
            TeacherRepository teacherRepository,
            ClassSubjectAssignmentRepository assignmentRepository,
            TimetableEntryRepository timetableEntryRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository) {
        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
    }

    /** Vrai si l'utilisateur courant est un enseignant, donc soumis au périmètre. */
    public boolean isRestricted() {
        return currentPrincipal().map(p -> Role.TEACHER.name().equals(p.role())).orElse(false);
    }

    /**
     * Périmètre de l'enseignant connecté, ou vide si l'utilisateur courant n'est pas restreint
     * (direction, vie scolaire… ou traitement hors requête HTTP).
     */
    public Optional<Perimeter> currentPerimeter() {
        Optional<AuthenticatedPrincipal> principal = currentPrincipal();
        if (principal.isEmpty() || !Role.TEACHER.name().equals(principal.get().role())) {
            return Optional.empty();
        }
        // Mémorisé pour la durée de la requête : une feuille d'appel de quarante élèves
        // vérifie quarante fois le périmètre, qui ne change pas entre-temps.
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.of(perimeterFor(principal.get().email()));
        }
        Object cached = attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (cached instanceof Perimeter perimeter) {
            return Optional.of(perimeter);
        }
        Perimeter perimeter = perimeterFor(principal.get().email());
        attributes.setAttribute(REQUEST_ATTRIBUTE, perimeter, RequestAttributes.SCOPE_REQUEST);
        return Optional.of(perimeter);
    }

    /** Utilisable en SpEL : {@code @teacherScope.canAccessClass(#id)}. */
    public boolean canAccessClass(Long classId) {
        return currentPerimeter().map(p -> p.canAccessClass(classId)).orElse(true);
    }

    public void requireClass(Long classId) {
        currentPerimeter().ifPresent(p -> {
            if (!p.canAccessClass(classId)) {
                deny("classe " + classId);
            }
        });
    }

    public void requireClassSubject(Long classId, Long subjectId) {
        currentPerimeter().ifPresent(p -> {
            if (!p.canAccessClassSubject(classId, subjectId)) {
                deny("classe " + classId + " / matière " + subjectId);
            }
        });
    }

    /** L'élève doit appartenir à une classe du périmètre (un élève sans classe est hors périmètre). */
    public void requireStudent(Long studentId) {
        currentPerimeter().ifPresent(p -> {
            if (!p.canAccessClass(classOfStudent(studentId))) {
                deny("élève " + studentId);
            }
        });
    }

    /**
     * Pour un enseignant : l'élève doit être inscrit dans CETTE classe (et la classe dans son
     * périmètre). Empêche de glisser dans l'appel de sa classe un élève d'une autre classe.
     */
    public void requireStudentInClass(Long studentId, Long classId) {
        currentPerimeter().ifPresent(p -> {
            Long studentClassId = classOfStudent(studentId);
            if (!p.canAccessClass(classId) || studentClassId == null || !studentClassId.equals(classId)) {
                deny("élève " + studentId + " hors de la classe " + classId);
            }
        });
    }

    public void requireStudentSubject(Long studentId, Long subjectId) {
        currentPerimeter().ifPresent(p -> {
            if (!p.canAccessClassSubject(classOfStudent(studentId), subjectId)) {
                deny("élève " + studentId + " / matière " + subjectId);
            }
        });
    }

    /** Un enseignant ne consulte que son propre emploi du temps. */
    public void requireOwnTeacher(Long teacherId) {
        currentPerimeter().ifPresent(p -> {
            if (!p.teacherIds().contains(teacherId)) {
                deny("enseignant " + teacherId);
            }
        });
    }

    private Long classOfStudent(Long studentId) {
        return studentRepository.findById(studentId).map(s -> s.getSchoolClassId()).orElse(null);
    }

    private Perimeter perimeterFor(String principalEmail) {
        String email = EmailNormalizer.normalize(principalEmail);
        List<Teacher> matches = email == null ? List.of() : teacherRepository.findActiveByNormalizedEmail(email);
        if (matches.size() > 1) {
            // Deux fiches actives pour une même adresse : impossible de savoir laquelle est ce
            // compte. Refus par défaut plutôt qu'union — l'union donnerait au compte les classes
            // d'un homonyme. À corriger dans les fiches enseignant (adresse unique par personne).
            log.warn("Rattachement enseignant ambigu : {} fiches actives pour le même e-mail (ids {}), périmètre vide",
                    matches.size(), matches.stream().map(Teacher::getId).sorted().toList());
            return new Perimeter(Set.of(), Set.of(), Set.of());
        }
        if (matches.isEmpty()) {
            return new Perimeter(Set.of(), Set.of(), Set.of());
        }
        Set<Long> teacherIds = Set.of(matches.get(0).getId());

        Set<String> pairs = new HashSet<>();
        Set<Long> classIds = new HashSet<>();
        for (ClassSubjectAssignment a : assignmentRepository.findAllByTeacherIdIn(teacherIds)) {
            pairs.add(pairKey(a.getClassId(), a.getSubjectId()));
            classIds.add(a.getClassId());
        }
        for (TimetableEntry e : timetableEntryRepository.findAllByTeacherIdIn(teacherIds)) {
            pairs.add(pairKey(e.getSchoolClassId(), e.getSubjectId()));
            classIds.add(e.getSchoolClassId());
        }
        List<SchoolClass> headed = schoolClassRepository.findAllByHeadTeacherIdIn(teacherIds);
        headed.forEach(c -> classIds.add(c.getId()));
        return new Perimeter(teacherIds, Set.copyOf(classIds), Set.copyOf(pairs));
    }

    private void deny(String resource) {
        Long userId = currentPrincipal().map(AuthenticatedPrincipal::subjectId).orElse(null);
        log.warn("Accès hors périmètre refusé : enseignant (user {}) sur {}", userId, resource);
        throw ApiException.forbidden(DENIED_CODE, "Cette ressource ne fait pas partie de vos classes");
    }

    private static Optional<AuthenticatedPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    private static String pairKey(Long classId, Long subjectId) {
        return classId + ":" + subjectId;
    }

    /**
     * Périmètre calculé d'un enseignant.
     *
     * @param teacherIds fiches {@code teachers} rattachées au compte
     * @param classIds classes accessibles (affectation, créneau ou professeur principal)
     * @param classSubjectPairs couples « classe:matière » enseignés (affectation ou créneau)
     */
    public record Perimeter(Set<Long> teacherIds, Set<Long> classIds, Set<String> classSubjectPairs) {

        public boolean canAccessClass(Long classId) {
            return classId != null && classIds.contains(classId);
        }

        public boolean canAccessClassSubject(Long classId, Long subjectId) {
            return classId != null && subjectId != null && classSubjectPairs.contains(pairKey(classId, subjectId));
        }
    }
}
