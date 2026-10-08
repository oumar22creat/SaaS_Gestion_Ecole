package com.schoolsaas.grade;

/**
 * Nature d'une évaluation, telle que la lit un bulletin ouest-africain : les devoirs de la
 * période d'un côté, la composition de fin de période de l'autre.
 *
 * <p>La distinction n'est pas cosmétique : elle commande la colonne où la note se range sur
 * le bulletin, et c'est elle que regarde une famille pour savoir si l'élève a tenu le jour
 * de l'examen ce qu'il promettait en classe.
 */
public enum ExamType {
    DEVOIR,
    COMPOSITION
}
