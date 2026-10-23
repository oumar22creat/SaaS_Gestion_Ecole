package com.schoolsaas.teacher.dto;

import com.schoolsaas.teacher.TeacherScope;
import java.util.List;

/**
 * Périmètre pédagogique de l'utilisateur connecté (ADR-030), pour que l'interface masque ce que
 * le serveur refuserait. Ne contient que des identifiants de classes et de matières : jamais
 * l'identité d'un autre enseignant.
 *
 * @param restricted vrai pour un enseignant ; faux pour les autres rôles du personnel, dont le
 *     périmètre est l'établissement entier (les listes sont alors vides et sans objet)
 * @param classIds classes accessibles (affectation, créneau ou professeur principal)
 * @param headClassIds classes dont il est professeur principal (bulletins : génération,
 *     appréciation générale, décision du conseil)
 * @param classSubjects couples (classe, matière) enseignés (évaluations, cahier de textes,
 *     appréciation d'une matière)
 */
public record TeachingScopeResponse(
        boolean restricted,
        List<Long> classIds,
        List<Long> headClassIds,
        List<TeacherScope.ClassSubject> classSubjects) {

    public static TeachingScopeResponse unrestricted() {
        return new TeachingScopeResponse(false, List.of(), List.of(), List.of());
    }

    public static TeachingScopeResponse from(TeacherScope.Perimeter perimeter) {
        return new TeachingScopeResponse(
                true,
                perimeter.classIds().stream().sorted().toList(),
                perimeter.headClassIds().stream().sorted().toList(),
                perimeter.classSubjects());
    }
}
