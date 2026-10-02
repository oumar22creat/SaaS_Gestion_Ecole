package com.schoolsaas.notification;

import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParent;
import com.schoolsaas.parent.StudentParentRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Traduit « cet élève » en comptes à notifier.
 *
 * <p>Le registre de notifications raisonne en identifiants de compte, alors que les modules
 * métier raisonnent en élèves. Faute de cette traduction, les absences étaient envoyées avec
 * une liste de destinataires vide : le chemin avait été écrit pour le SMS, qui vise les
 * tuteurs par numéro de téléphone et n'a donc jamais eu besoin de comptes.
 *
 * <p>Une fiche parent ou élève n'a pas forcément d'accès à l'application (ADR-010) : la liste
 * peut légitimement revenir vide, et c'est alors au SMS de prendre le relais.
 */
@Component
public class FamilyRecipients {

    private final StudentParentRepository studentParentRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;

    public FamilyRecipients(
            StudentParentRepository studentParentRepository,
            ParentRepository parentRepository,
            StudentRepository studentRepository) {
        this.studentParentRepository = studentParentRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
    }

    /** Les comptes des tuteurs de cet élève. */
    public List<Long> tuteursDe(Long studentId) {
        List<StudentParent> liens = studentParentRepository.findAllByStudentId(studentId);
        if (liens.isEmpty()) {
            return List.of();
        }
        return parentRepository.findAllById(liens.stream().map(StudentParent::getParentId).toList()).stream()
                .map(Parent::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Les comptes de toutes les familles d'une classe, élèves compris.
     *
     * <p>Pour un devoir, l'information s'adresse à la classe entière. Les doublons sont écartés :
     * deux enfants d'une même fratrie dans la même classe partagent leurs tuteurs, qui ne
     * doivent pas recevoir deux fois la même annonce.
     */
    public List<Long> famillesDeLaClasse(Long schoolClassId) {
        return studentRepository.findAllBySchoolClassId(schoolClassId).stream()
                .filter(Student::isActive)
                .flatMap(eleve -> familleDe(eleve.getId()).stream())
                .distinct()
                .toList();
    }

    /**
     * Les comptes des tuteurs, et celui de l'élève lui-même.
     *
     * <p>Pour une note ou un document, l'élève est concerné au même titre que sa famille. Pour
     * une absence en revanche, le message est rédigé à l'intention d'un tuteur — l'y adjoindre
     * lui enverrait un texte qui ne lui est pas adressé.
     */
    public List<Long> familleDe(Long studentId) {
        List<Long> comptes = new ArrayList<>(tuteursDe(studentId));
        studentRepository
                .findById(studentId)
                .map(eleve -> eleve.getUserId())
                .filter(Objects::nonNull)
                .filter(compte -> !comptes.contains(compte))
                .ifPresent(comptes::add);
        return List.copyOf(comptes);
    }
}
