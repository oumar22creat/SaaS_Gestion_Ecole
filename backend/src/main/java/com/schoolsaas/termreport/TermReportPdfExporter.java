package com.schoolsaas.termreport;

import static com.schoolsaas.officialdoc.OfficialDocumentLayout.HAUTEUR_LIGNE;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.MARGE;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.ecrire;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.ecrireCentre;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.gras;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.normal;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.rectangle;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.tronquer;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.officialdoc.OfficialDocumentLayout;
import com.schoolsaas.tenant.Tenant;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

/**
 * Rapport trimestriel d'une classe, en PDF, à adresser à la tutelle.
 *
 * <p>Même en-tête et même signature que le bulletin : les deux documents partent du même
 * établissement sur le même trimestre, et une inspection qui les reçoit tous les deux doit y
 * lire les mêmes mentions. C'est pour cela que la mise en page est partagée et non recopiée.
 */
@Component
public class TermReportPdfExporter {

    public byte[] export(TermReport rapport, Tenant tenant, byte[] logo) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            float largeur = page.getMediaBox().getWidth();
            float hauteur = page.getMediaBox().getHeight();

            PDType1Font normal = normal();
            PDType1Font gras = gras();

            try (PDPageContentStream c = new PDPageContentStream(document, page)) {
                float y = OfficialDocumentLayout.enTete(document, c, tenant, logo, largeur, hauteur);
                y = OfficialDocumentLayout.titre(
                        c,
                        "RAPPORT TRIMESTRIEL - " + rapport.periodLabel().toUpperCase(Locale.FRENCH),
                        "ANNÉE SCOLAIRE : " + rapport.schoolYear(),
                        largeur,
                        y);
                y = identite(c, rapport, normal, gras, largeur, y);
                y = effectifEtResultats(c, rapport, normal, gras, largeur, y - 6);
                y = tableauMatieres(c, rapport, normal, gras, largeur, y - 12);
                y = repartition(c, rapport, normal, gras, largeur, y - 12);
                y = assiduiteEtDiscipline(c, rapport, normal, gras, largeur, y - 12);
                OfficialDocumentLayout.signature(c, tenant, largeur, y - 26);
                OfficialDocumentLayout.piedDePage(c, tenant, largeur);
            }

            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw ApiException.unprocessable(
                    "PDF_GENERATION_FAILED", "Impossible de générer le rapport trimestriel");
        }
    }

    private float identite(
            PDPageContentStream c, TermReport rapport, PDType1Font normal, PDType1Font gras, float largeur, float y)
            throws IOException {
        float colonneDroite = largeur / 2 + 20;
        paire(c, gras, normal, MARGE, y, "CLASSE :", rapport.className());
        paire(c, gras, normal, colonneDroite, y, "PROFESSEUR PRINCIPAL :", rapport.headTeacherName());
        paire(c, gras, normal, MARGE, y - HAUTEUR_LIGNE, "PÉRIODE :",
                rapport.periodFrom().format(OfficialDocumentLayout.JOUR) + " au "
                        + rapport.periodTo().format(OfficialDocumentLayout.JOUR));
        return y - HAUTEUR_LIGNE * 2 - 4;
    }

    private void paire(
            PDPageContentStream c, PDType1Font gras, PDType1Font normal, float x, float y, String libelle,
            String valeur) throws IOException {
        ecrire(c, gras, 9.5f, x, y, libelle);
        ecrire(c, normal, 9.5f, x + OfficialDocumentLayout.largeurTexte(gras, 9.5f, libelle) + 6, y,
                valeur == null ? "-" : valeur);
    }

    private float effectifEtResultats(
            PDPageContentStream c, TermReport rapport, PDType1Font normal, PDType1Font gras, float largeur, float y)
            throws IOException {
        TermReport.Effectif e = rapport.effectif();
        TermReport.Resultats r = rapport.resultats();
        String[][] cases = {
            {"Effectif", String.valueOf(e.total())},
            {"Filles", String.valueOf(e.filles())},
            {"Garçons", String.valueOf(e.garcons())},
            {"Évalués", String.valueOf(e.evalues())},
            {"Moyenne de classe", note(r.moyenneClasse()) + " / 20"},
            {"Moyenne ≥ 10", r.admis() + " élève(s)"},
            {"Taux de réussite", pourcent(r.tauxReussite())},
            {"Meilleure / plus faible", note(r.meilleureMoyenne()) + " / " + note(r.plusFaibleMoyenne())},
        };
        return grilleDeCases(c, cases, 4, normal, gras, largeur, y);
    }

    /** Une grille de cases « libellé / valeur », n par ligne. */
    private float grilleDeCases(
            PDPageContentStream c, String[][] cases, int parLigne, PDType1Font normal, PDType1Font gras,
            float largeur, float y) throws IOException {
        float largeurUtile = largeur - 2 * MARGE;
        float largeurCase = largeurUtile / parLigne;
        float hauteurCase = 30;
        float ligneY = y;
        for (int i = 0; i < cases.length; i++) {
            if (i % parLigne == 0) {
                ligneY -= hauteurCase;
            }
            float x = MARGE + (i % parLigne) * largeurCase;
            rectangle(c, x, ligneY, largeurCase, hauteurCase);
            c.setNonStrokingColor(new Color(0x66, 0x66, 0x66));
            ecrire(c, normal, 6.5f, x + 6, ligneY + hauteurCase - 11,
                    cases[i][0].toUpperCase(Locale.FRENCH));
            c.setNonStrokingColor(Color.BLACK);
            ecrire(c, gras, 10, x + 6, ligneY + 8, tronquer(gras, 10, cases[i][1], largeurCase - 12));
        }
        return ligneY;
    }

    private float tableauMatieres(
            PDPageContentStream c, TermReport rapport, PDType1Font normal, PDType1Font gras, float largeur, float y)
            throws IOException {
        ecrire(c, gras, 10, MARGE, y, "RÉSULTATS PAR DISCIPLINE");
        y -= 8;

        float largeurUtile = largeur - 2 * MARGE;
        float[] colonnes = {largeurUtile - 300, 40, 60, 50, 50, 50, 50};
        String[] entetes = {"DISCIPLINE", "COEF", "MOYENNE", "MAX", "MIN", "≥ 10", "RÉUSSITE"};

        float ligneY = y - HAUTEUR_LIGNE;
        float x = MARGE;
        for (int i = 0; i < colonnes.length; i++) {
            rectangle(c, x, ligneY, colonnes[i], HAUTEUR_LIGNE);
            ecrireCentre(c, gras, 7.5f, x, colonnes[i], ligneY + 5, entetes[i]);
            x += colonnes[i];
        }

        for (TermReport.LigneMatiere matiere : rapport.matieres()) {
            ligneY -= HAUTEUR_LIGNE;
            String[] cellules = {
                matiere.matiere().toUpperCase(Locale.FRENCH),
                String.valueOf(matiere.coefficient()),
                note(matiere.moyenne()),
                note(matiere.note_max()),
                note(matiere.note_min()),
                matiere.atteignentLaMoyenne() + "/" + matiere.notes(),
                pourcent(matiere.tauxReussite()),
            };
            float cx = MARGE;
            for (int i = 0; i < colonnes.length; i++) {
                rectangle(c, cx, ligneY, colonnes[i], HAUTEUR_LIGNE);
                if (i == 0) {
                    ecrire(c, gras, 8.5f, cx + 5, ligneY + 5,
                            tronquer(gras, 8.5f, cellules[i], colonnes[i] - 10));
                } else {
                    // Une moyenne sous 10 se repère d'un coup d'œil : c'est la discipline
                    // sur laquelle l'inspection demandera un plan de redressement.
                    boolean faible = i == 2 && matiere.moyenne() != null && matiere.moyenne() < 10;
                    if (faible) {
                        c.setNonStrokingColor(new Color(0xB0, 0x2A, 0x1F));
                    }
                    ecrireCentre(c, faible ? gras : normal, 8.5f, cx, colonnes[i], ligneY + 5, cellules[i]);
                    c.setNonStrokingColor(Color.BLACK);
                }
                cx += colonnes[i];
            }
        }
        return ligneY;
    }

    private float repartition(
            PDPageContentStream c, TermReport rapport, PDType1Font normal, PDType1Font gras, float largeur, float y)
            throws IOException {
        ecrire(c, gras, 10, MARGE, y, "RÉPARTITION DES MOYENNES");
        y -= 8;

        List<TermReport.Tranche> tranches = rapport.repartition();
        float largeurUtile = largeur - 2 * MARGE;
        float largeurCase = largeurUtile / tranches.size();
        float hauteurCase = 34;
        float ligneY = y - hauteurCase;
        int maximum = tranches.stream().mapToInt(TermReport.Tranche::effectif).max().orElse(0);

        for (int i = 0; i < tranches.size(); i++) {
            TermReport.Tranche tranche = tranches.get(i);
            float x = MARGE + i * largeurCase;
            // Barre proportionnelle en fond : un tableau de chiffres ne montre pas la forme
            // de la classe, et c'est précisément ce que la tutelle regarde en premier.
            if (maximum > 0 && tranche.effectif() > 0) {
                float hauteurBarre = (hauteurCase - 12) * tranche.effectif() / maximum;
                c.setNonStrokingColor(new Color(0xDC, 0xE8, 0xE4));
                c.addRect(x + 1, ligneY + 1, largeurCase - 2, hauteurBarre);
                c.fill();
                c.setNonStrokingColor(Color.BLACK);
            }
            rectangle(c, x, ligneY, largeurCase, hauteurCase);
            ecrireCentre(c, normal, 6.5f, x, largeurCase, ligneY + hauteurCase - 10, tranche.libelle());
            ecrireCentre(c, gras, 11, x, largeurCase, ligneY + 12, String.valueOf(tranche.effectif()));
            ecrireCentre(c, normal, 6, x, largeurCase, ligneY + 4, pourcent(tranche.part()));
        }
        return ligneY;
    }

    private float assiduiteEtDiscipline(
            PDPageContentStream c, TermReport rapport, PDType1Font normal, PDType1Font gras, float largeur, float y)
            throws IOException {
        ecrire(c, gras, 10, MARGE, y, "ASSIDUITÉ ET VIE SCOLAIRE");
        TermReport.Assiduite a = rapport.assiduite();
        TermReport.Discipline d = rapport.discipline();
        String[][] cases = {
            {"Taux de présence", pourcent(a.tauxPresence())},
            {"Absences", a.absences() + " dont " + a.absencesJustifiees() + " justifiées"},
            {"Retards", String.valueOf(a.retards())},
            {"Incidents", d.incidents() + " / " + d.sanctions() + " sanction(s)"},
        };
        return grilleDeCases(c, cases, 4, normal, gras, largeur, y - 8);
    }

    private String note(Double valeur) {
        return valeur == null ? "-" : String.format(Locale.FRENCH, "%.2f", valeur);
    }

    private String pourcent(Double valeur) {
        return valeur == null ? "-" : String.format(Locale.FRENCH, "%.1f %%", valeur);
    }
}
