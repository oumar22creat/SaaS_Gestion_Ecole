package com.schoolsaas.reportcard;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.officialdoc.OfficialDocumentLayout;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.student.Student;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.tenant.Tenant;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

/**
 * Bulletin au format officiel ouest-africain, d'après la maquette fournie par l'exploitant
 * (Groupe Scolaire PIA, Dakar) : en-tête administratif, tableau encadré des disciplines avec
 * devoirs et composition, récapitulatif, signature du directeur.
 *
 * <p>Pourquoi un tableau tracé au trait plutôt que des colonnes de texte alignées, comme le
 * faisait la version précédente : ce document est photocopié, agrafé au dossier, parfois
 * présenté à une administration. Sans filets, une ligne se lit de travers dès que deux
 * matières ont des noms de longueur différente, et rien ne distingue la moyenne de la
 * composition. La grille n'est pas une décoration, c'est ce qui rend le document lisible.
 *
 * <p>Les mentions officielles (État, académie, inspection, directeur, ville) sont saisies par
 * l'établissement : voir {@link Tenant}. Un établissement qui n'a rien renseigné obtient un
 * bulletin correct, simplement sans ces lignes — une école privée malienne n'a pas les mêmes
 * en-têtes qu'une école publique sénégalaise.
 */
@Component
public class ReportCardPdfExporter {

    private static final float MARGE = OfficialDocumentLayout.MARGE;
    private static final float HAUTEUR_LIGNE = OfficialDocumentLayout.HAUTEUR_LIGNE;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Largeurs des colonnes du tableau, dans l'ordre de la maquette. */
    private static final float COL_DISCIPLINE = 132;
    private static final float COL_COEF = 38;
    private static final float COL_NOTE = 58;
    private static final float COL_MOYENNE = 62;
    private static final float COL_APPRECIATION = 127;

    private static final float LOGO_MAX = 54;

    public byte[] export(
            ReportCard reportCard, List<ReportCardEntry> entries, Student student, SchoolClass schoolClass,
            Map<Long, Subject> subjectsById, Tenant tenant, byte[] logo) {
        try (PDDocument document = new PDDocument()) {
            dessinerPage(document, reportCard, entries, student, schoolClass, subjectsById, tenant, logo);
            return enregistrer(document);
        } catch (IOException e) {
            throw ApiException.unprocessable("PDF_GENERATION_FAILED", "Impossible de générer le PDF du bulletin");
        }
    }

    /**
     * Toute une classe en un seul PDF, un bulletin par page.
     *
     * <p>Un fichier et non une archive de cent : le secrétariat imprime une liasse, la coupe
     * et la distribue. Télécharger cent pièces jointes pour les ouvrir une à une à l'heure
     * des conseils de classe n'est pas un travail, c'est une punition.
     */
    public byte[] exportBatch(List<Bulletin> bulletins, SchoolClass schoolClass, Tenant tenant, byte[] logo) {
        if (bulletins.isEmpty()) {
            throw ApiException.unprocessable(
                    "NO_REPORT_CARD_TO_PRINT", "Aucun bulletin à imprimer pour cette classe et cette période");
        }
        try (PDDocument document = new PDDocument()) {
            for (Bulletin bulletin : bulletins) {
                dessinerPage(document, bulletin.reportCard(), bulletin.entries(), bulletin.student(), schoolClass,
                        bulletin.subjectsById(), tenant, logo);
            }
            return enregistrer(document);
        } catch (IOException e) {
            throw ApiException.unprocessable("PDF_GENERATION_FAILED", "Impossible de générer les bulletins de la classe");
        }
    }

    /** Un bulletin et tout ce qu'il faut pour le dessiner, pour l'impression en liasse. */
    public record Bulletin(
            ReportCard reportCard, List<ReportCardEntry> entries, Student student, Map<Long, Subject> subjectsById) {
    }

    private byte[] enregistrer(PDDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        return out.toByteArray();
    }

    private void dessinerPage(
            PDDocument document, ReportCard reportCard, List<ReportCardEntry> entries, Student student,
            SchoolClass schoolClass, Map<Long, Subject> subjectsById, Tenant tenant, byte[] logo) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        float largeur = page.getMediaBox().getWidth();
        float hauteur = page.getMediaBox().getHeight();

        PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDPageContentStream c = new PDPageContentStream(document, page)) {
            float y = OfficialDocumentLayout.enTete(document, c, tenant, logo, largeur, hauteur);
            y = OfficialDocumentLayout.titre(
                    c,
                    "BULLETIN DE NOTES - " + reportCard.getPeriodLabel().toUpperCase(Locale.FRENCH),
                    "ANNÉE SCOLAIRE : " + anneeScolaire(reportCard),
                    largeur,
                    y);
            y = identiteEleve(c, student, schoolClass, normal, gras, largeur, y);
            y = tableauDisciplines(c, entries, subjectsById, normal, gras, largeur, y - 6);
            y = recapitulatif(c, reportCard, entries, normal, gras, largeur, y - 10);
            OfficialDocumentLayout.signature(c, tenant, largeur, y - 24);
            OfficialDocumentLayout.piedDePage(c, tenant, largeur);
        }
    }

    /**
     * L'année scolaire se déduit des bornes de la période : « 2026-2027 ». Une période à
     * cheval sur deux années civiles est la norme, pas l'exception.
     */
    private String anneeScolaire(ReportCard reportCard) {
        LocalDate debut = reportCard.getPeriodFrom();
        LocalDate fin = reportCard.getPeriodTo();
        int premiere = debut.getMonthValue() >= 8 ? debut.getYear() : debut.getYear() - 1;
        int seconde = Math.max(premiere + 1, fin.getYear());
        return premiere + " - " + seconde;
    }

    private float identiteEleve(
            PDPageContentStream c, Student student, SchoolClass schoolClass, PDType1Font normal, PDType1Font gras,
            float largeur, float y) throws IOException {
        float colonneDroite = largeur / 2 + 30;
        paire(c, gras, normal, MARGE + 18, y, "PRÉNOM(S) :", student.getFirstName());
        paire(c, gras, normal, colonneDroite, y, "NOM :", student.getLastName());
        paire(c, gras, normal, MARGE + 18, y - HAUTEUR_LIGNE, "MATRICULE :", student.getStudentNumber());
        paire(c, gras, normal, colonneDroite, y - HAUTEUR_LIGNE, "CLASSE :", schoolClass.getName());
        return y - HAUTEUR_LIGNE * 2 - 6;
    }

    private void paire(
            PDPageContentStream c, PDType1Font gras, PDType1Font normal, float x, float y, String libelle, String valeur)
            throws IOException {
        OfficialDocumentLayout.ecrire(c, gras, 10, x, y, libelle);
        OfficialDocumentLayout.ecrire(c, normal, 10, x + OfficialDocumentLayout.largeurTexte(gras, 10, libelle) + 6, y, valeur == null ? "—" : valeur);
    }

    private float tableauDisciplines(
            PDPageContentStream c, List<ReportCardEntry> entries, Map<Long, Subject> subjectsById, PDType1Font normal,
            PDType1Font gras, float largeur, float y) throws IOException {
        float[] colonnes = {COL_DISCIPLINE, COL_COEF, COL_NOTE, COL_NOTE, COL_NOTE, COL_MOYENNE, COL_APPRECIATION};
        float largeurTableau = 0;
        for (float col : colonnes) {
            largeurTableau += col;
        }
        float x0 = (largeur - largeurTableau) / 2;

        // Double bandeau d'en-tête : « NOTES » chapeaute les trois colonnes de notes, comme
        // sur la maquette. Les autres intitulés sont à cheval sur les deux rangées.
        float hautBandeau = y;
        float milieuBandeau = y - HAUTEUR_LIGNE;
        float basBandeau = y - HAUTEUR_LIGNE * 2;

        float x = x0;
        c.setLineWidth(0.8f);
        OfficialDocumentLayout.rectangle(c, x, basBandeau, colonnes[0], HAUTEUR_LIGNE * 2);
        OfficialDocumentLayout.ecrireCentre(c, gras, 8.5f, x, colonnes[0], basBandeau + HAUTEUR_LIGNE / 2 + 1, "DISCIPLINES");
        x += colonnes[0];
        OfficialDocumentLayout.rectangle(c, x, basBandeau, colonnes[1], HAUTEUR_LIGNE * 2);
        OfficialDocumentLayout.ecrireCentre(c, gras, 8.5f, x, colonnes[1], basBandeau + HAUTEUR_LIGNE / 2 + 1, "COEF");
        x += colonnes[1];

        float largeurNotes = colonnes[2] + colonnes[3] + colonnes[4];
        OfficialDocumentLayout.rectangle(c, x, milieuBandeau, largeurNotes, HAUTEUR_LIGNE);
        OfficialDocumentLayout.ecrireCentre(c, gras, 8.5f, x, largeurNotes, milieuBandeau + 5, "NOTES");
        String[] intitulesNotes = {"DEVOIR 1", "DEVOIR 2", "COMP"};
        for (int i = 0; i < intitulesNotes.length; i++) {
            float l = colonnes[2 + i];
            OfficialDocumentLayout.rectangle(c, x, basBandeau, l, HAUTEUR_LIGNE);
            OfficialDocumentLayout.ecrireCentre(c, gras, 7.5f, x, l, basBandeau + 5, intitulesNotes[i]);
            x += l;
        }
        OfficialDocumentLayout.rectangle(c, x, basBandeau, colonnes[5], HAUTEUR_LIGNE * 2);
        OfficialDocumentLayout.ecrireCentre(c, gras, 8.5f, x, colonnes[5], basBandeau + HAUTEUR_LIGNE / 2 + 1, "MOYENNE");
        x += colonnes[5];
        OfficialDocumentLayout.rectangle(c, x, basBandeau, colonnes[6], HAUTEUR_LIGNE * 2);
        OfficialDocumentLayout.ecrireCentre(c, gras, 8.5f, x, colonnes[6], basBandeau + HAUTEUR_LIGNE / 2 + 1, "APPRÉCIATION");

        float ligneY = basBandeau;
        for (ReportCardEntry entry : entries) {
            ligneY -= HAUTEUR_LIGNE;
            Subject matiere = subjectsById.get(entry.getSubjectId());
            String nom = matiere != null ? matiere.getName() : "Matière #" + entry.getSubjectId();
            String[] cellules = {
                OfficialDocumentLayout.tronquer(gras, 8.5f, nom.toUpperCase(Locale.FRENCH), colonnes[0]),
                String.valueOf(entry.getCoefficient()),
                note(entry.getAssignmentOneScore()),
                note(entry.getAssignmentTwoScore()),
                note(entry.getExamScore()),
                note(entry.getAverage()),
                OfficialDocumentLayout.tronquer(normal, 8f, appreciation(entry), colonnes[6]),
            };
            float cx = x0;
            for (int i = 0; i < colonnes.length; i++) {
                OfficialDocumentLayout.rectangle(c, cx, ligneY, colonnes[i], HAUTEUR_LIGNE);
                PDType1Font police = i == 0 ? gras : normal;
                float taille = i == 6 ? 7.5f : 8.5f;
                OfficialDocumentLayout.ecrireCentre(c, police, taille, cx, colonnes[i], ligneY + 5, cellules[i]);
                cx += colonnes[i];
            }
        }
        return ligneY;
    }

    /**
     * Appréciation littérale déduite de la moyenne quand l'enseignant n'en a pas saisi.
     * Un bulletin dont la colonne « appréciation » est vide d'un bout à l'autre donne
     * l'impression d'un devoir bâclé par l'école, pas par l'élève.
     */
    private String appreciation(ReportCardEntry entry) {
        if (entry.getTeacherComment() != null && !entry.getTeacherComment().isBlank()) {
            return entry.getTeacherComment();
        }
        Double moyenne = entry.getAverage();
        if (moyenne == null) {
            return "";
        }
        if (moyenne >= 16) {
            return "Excellent";
        }
        if (moyenne >= 14) {
            return "Très bien";
        }
        if (moyenne >= 12) {
            return "Bien";
        }
        if (moyenne >= 10) {
            return "Assez bien";
        }
        if (moyenne >= 8) {
            return "Passable";
        }
        return "Insuffisant";
    }

    private float recapitulatif(
            PDPageContentStream c, ReportCard reportCard, List<ReportCardEntry> entries, PDType1Font normal,
            PDType1Font gras, float largeur, float y) throws IOException {
        float largeurTableau = COL_DISCIPLINE + COL_COEF + COL_NOTE * 3 + COL_MOYENNE + COL_APPRECIATION;
        float x0 = (largeur - largeurTableau) / 2;
        float largeurValeur = 150;
        float largeurLibelle = largeurTableau - largeurValeur;

        String[][] lignes = {
            {"TOTAL DES COEFFICIENTS :", String.valueOf(
                    entries.stream().mapToInt(ReportCardEntry::getCoefficient).sum())},
            {"MOYENNE DE LA PÉRIODE :", note(reportCard.getGeneralAverage()) + " / 20"},
            {"RANG :", rang(reportCard)},
            {"ABSENCES / RETARDS :", reportCard.getAbsenceCount() + " / " + reportCard.getLateCount()},
            {"DÉCISION DU CONSEIL :", valeurOuTiret(reportCard.getCouncilDecision())},
            {"APPRÉCIATION DU CONSEIL :", valeurOuTiret(reportCard.getGeneralComment())},
        };
        float ligneY = y;
        for (String[] ligne : lignes) {
            ligneY -= HAUTEUR_LIGNE;
            OfficialDocumentLayout.rectangle(c, x0, ligneY, largeurLibelle, HAUTEUR_LIGNE);
            OfficialDocumentLayout.ecrire(c, gras, 8.5f, x0 + 5, ligneY + 5, ligne[0]);
            OfficialDocumentLayout.rectangle(c, x0 + largeurLibelle, ligneY, largeurValeur, HAUTEUR_LIGNE);
            OfficialDocumentLayout.ecrire(c, normal, 8.5f, x0 + largeurLibelle + 5, ligneY + 5,
                    OfficialDocumentLayout.tronquer(normal, 8.5f, ligne[1], largeurValeur - 10));
        }
        return ligneY;
    }

    private String note(Double valeur) {
        return valeur == null ? "" : String.format(Locale.FRENCH, "%.2f", valeur);
    }

    private String valeurOuTiret(String valeur) {
        return OfficialDocumentLayout.rempli(valeur) ? valeur : "—";
    }

    /** « 3e sur 42 ». Un élève sans moyenne n'est pas classé : le dire évite le soupçon d'erreur. */
    private String rang(ReportCard reportCard) {
        if (reportCard.getRankInClass() == null || reportCard.getClassSize() == null) {
            return "non classé";
        }
        int rang = reportCard.getRankInClass();
        return (rang == 1 ? "1er" : rang + "e") + " sur " + reportCard.getClassSize();
    }

    private static boolean rempli(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private Color couleur(String hex) {
        if (hex == null || !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            return new Color(0x0f, 0x5c, 0x4c);
        }
        return new Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }
}
