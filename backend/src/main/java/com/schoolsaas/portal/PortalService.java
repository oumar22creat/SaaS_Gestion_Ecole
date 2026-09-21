package com.schoolsaas.portal;

import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserService;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParentRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Portail parent/élève. Tout le reste de l'application est écrit pour le personnel, qui voit
 * l'établissement entier ; ici au contraire chaque requête est réduite aux seuls élèves que
 * l'appelant a le droit de consulter.
 *
 * <p>Règle centrale : l'identifiant d'élève reçu dans l'URL n'est <b>jamais</b> digne de
 * confiance. Il est systématiquement confronté à la liste calculée depuis le compte connecté,
 * sans quoi un parent pourrait lire le dossier de l'enfant d'une autre famille en changeant un
 * chiffre dans l'adresse. Le filtre tenant d'Hibernate ne protège pas de cela : les deux
 * familles sont dans le même établissement.
 */
@Service
public class PortalService {

    private final UserService userService;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;

    public PortalService(
            UserService userService,
            StudentRepository studentRepository,
            ParentRepository parentRepository,
            StudentParentRepository studentParentRepository) {
        this.userService = userService;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.studentParentRepository = studentParentRepository;
    }

    /**
     * Élèves consultables par le compte connecté : ses enfants s'il est parent, lui-même s'il
     * est élève. Un compte orphelin (fiche supprimée entre-temps) obtient une liste vide
     * plutôt qu'une erreur obscure.
     */
    public List<Student> accessibleStudents() {
        User me = userService.getCurrentUser();
        if (me.getRole() == Role.STUDENT) {
            return studentRepository.findByUserId(me.getId()).map(List::of).orElseGet(List::of);
        }
        if (me.getRole() == Role.PARENT) {
            Parent parent = parentRepository.findByUserId(me.getId()).orElse(null);
            if (parent == null) {
                return List.of();
            }
            List<Long> childIds = studentParentRepository.findAllByParentId(parent.getId()).stream()
                    .map(link -> link.getStudentId())
                    .toList();
            return studentRepository.findAllById(childIds);
        }
        throw ApiException.forbidden("PORTAL_NOT_AVAILABLE", "Ce portail est réservé aux élèves et aux parents");
    }

    /**
     * Vérifie que l'élève demandé fait bien partie du périmètre du compte connecté. Renvoie
     * "introuvable" plutôt qu'"interdit" : répondre 403 confirmerait à l'appelant que cet
     * élève existe dans l'établissement, ce qui est déjà une fuite d'information.
     */
    public Student requireAccessibleStudent(Long studentId) {
        return accessibleStudents().stream()
                .filter(student -> student.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
    }
}
