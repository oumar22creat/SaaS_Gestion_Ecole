package com.schoolsaas.termreport;

import java.util.List;

/**
 * Rapport trimestriel d'une classe, tel que l'établissement l'adresse à sa tutelle.
 *
 * <p>Ce que le ministère demande n'est pas la liste des élèves : c'est la santé de la
 * classe. D'où des agrégats et non des noms — effectif, moyenne, taux de réussite,
 * répartition, assiduité, discipline. Un rapport nominatif serait à la fois inutile à
 * l'administration et une diffusion de données d'enfants hors de l'école.
 */
public record TermReport(
        String className,
        String headTeacherName,
        String periodLabel,
        String schoolYear,
        java.time.LocalDate periodFrom,
        java.time.LocalDate periodTo,
        Effectif effectif,
        Resultats resultats,
        List<LigneMatiere> matieres,
        List<Tranche> repartition,
        Assiduite assiduite,
        Discipline discipline) {

    /** Un effectif se lit par sexe : c'est la première ventilation que demande la tutelle. */
    public record Effectif(int total, int filles, int garcons, int evalues) {
    }

    /**
     * @param evaluesSansNote élèves de la classe sans aucune note sur la période. Comptés à
     *                        part plutôt que noyés dans l'échec : un élève non évalué n'a
     *                        pas échoué, et le confondre fausse le taux de réussite.
     */
    public record Resultats(
            Double moyenneClasse,
            Double meilleureMoyenne,
            Double plusFaibleMoyenne,
            int admis,
            Double tauxReussite,
            int evaluesSansNote) {
    }

    public record LigneMatiere(
            String matiere,
            int coefficient,
            Double moyenne,
            Double note_max,
            Double note_min,
            int notes,
            int atteignentLaMoyenne,
            Double tauxReussite) {
    }

    /** Une tranche de moyennes et l'effectif qui s'y trouve : « 10 à 12 : 7 élèves ». */
    public record Tranche(String libelle, int effectif, Double part) {
    }

    public record Assiduite(int absences, int retards, int absencesJustifiees, Double tauxPresence) {
    }

    public record Discipline(int incidents, int sanctions, int observationsPositives) {
    }
}
