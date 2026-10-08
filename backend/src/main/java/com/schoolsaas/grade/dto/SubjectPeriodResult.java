package com.schoolsaas.grade.dto;

/**
 * Résultat d'un élève dans une matière sur une période, détaillé comme le bulletin l'affiche
 * (cf. maquette : colonnes DEVOIR 1, DEVOIR 2, COMP, MOYENNE).
 *
 * <p>Toutes les notes sont ramenées sur 20, quel que soit le barème de l'évaluation : le
 * bulletin juxtapose un devoir noté sur 10 et une composition sur 20, et les comparer tels
 * quels n'aurait aucun sens pour la famille qui les lit côte à côte.
 *
 * <p>Un champ nul signifie que l'évaluation n'a pas eu lieu, ou que l'élève y était absent.
 * Une case vide se lit mieux qu'un zéro, qui passerait pour une note.
 */
public record SubjectPeriodResult(
        Double assignmentOne, Double assignmentTwo, Double examScore, Double average, int gradeCount) {

    public static final SubjectPeriodResult EMPTY = new SubjectPeriodResult(null, null, null, null, 0);
}
