package com.schoolsaas.paperwork;

import static com.schoolsaas.officialdoc.OfficialDocumentLayout.HAUTEUR_LIGNE;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.MARGE;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.ecrire;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.ecrireCentre;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.gras;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.largeurTexte;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.normal;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.rectangle;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.rempli;
import static com.schoolsaas.officialdoc.OfficialDocumentLayout.tronquer;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.MoneyFormat;
import com.schoolsaas.officialdoc.OfficialDocumentLayout;
import com.schoolsaas.tenant.Tenant;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

/**
 * Fiche d'inscription d'un élève : la pièce que le secrétariat imprime au guichet, fait
 * signer au responsable légal et classe au dossier.
 *
 * <p>Elle est pré-remplie de ce que l'application sait, et laisse des lignes vides pour ce
 * qu'elle ignore — lieu de naissance, adresse, profession du tuteur. Ces champs figurent sur
 * toutes les fiches des écoles et ne sont pas en base : plutôt que d'inventer des colonnes
 * que personne ne saisirait, la fiche réserve la place pour les écrire à la main. Une fiche
 * amputée de ces lignes serait refaite à la main de bout en bout, donc inutile.
 */
@Component
public class EnrollmentFormPdfExporter {

    private static final float PHOTO_LARGEUR = 85;
    private static final float PHOTO_HAUTEUR = 108;

    /** Une fiche, et tout ce qu'il faut pour la remplir. */
    public record Donnees(
            String nom,
            String prenom,
            String matricule,
            LocalDate dateNaissance,
            String sexe,
            String classe,
            String anneeScolaire,
            byte[] photo,
            List<Responsable> responsables,
            List<Frais> frais) {
    }

    public record Responsable(String nom, String lien, String telephone, String courriel, boolean principal) {
    }

    public record Frais(String libelle, LocalDate echeance, long montantCentimes, String devise) {
    }

    public byte[] export(Donnees donnees, Tenant tenant, byte[] logo) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            float largeur = page.getMediaBox().getWidth();
            float hauteur = page.getMediaBox().getHeight();

            try (PDPageContentStream c = new PDPageContentStream(document, page)) {
                float y = OfficialDocumentLayout.enTete(document, c, tenant, logo, largeur, hauteur);
                y = OfficialDocumentLayout.titre(
                        c, "FICHE D'INSCRIPTION", "ANNÉE SCOLAIRE : " + donnees.anneeScolaire(), largeur, y);
                y = eleve(document, c, donnees, largeur, y - 4);
                y = responsables(c, donnees, largeur, y - 14);
                y = frais(c, donnees, largeur, y - 14);
                y = engagement(c, largeur, y - 14);
                signatures(c, tenant, largeur, y - 18);
                OfficialDocumentLayout.piedDePage(c, tenant, largeur);
            }

            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw ApiException.unprocessable(
                    "PDF_GENERATION_FAILED", "Impossible de générer la fiche d'inscription");
        }
    }

    private float eleve(PDDocument document, PDPageContentStream c, Donnees d, float largeur, float y)
            throws IOException {
        PDType1Font normal = normal();
        PDType1Font gras = gras();
        titreSection(c, "ÉLÈVE", y);
        // 20 et non 10 : le libellé du premier champ venait se coller sous le titre de
        // section, au point qu'on lisait « ÉLÈVE Nom » sur une seule ligne apparente.
        y -= 20;

        float xPhoto = largeur - MARGE - PHOTO_LARGEUR;
        float yPhoto = y - PHOTO_HAUTEUR;
        if (d.photo() != null) {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, d.photo(), "portrait");
            float echelle = Math.max(PHOTO_LARGEUR / image.getWidth(), PHOTO_HAUTEUR / image.getHeight());
            float l = image.getWidth() * echelle;
            float h = image.getHeight() * echelle;
            c.saveGraphicsState();
            c.addRect(xPhoto, yPhoto, PHOTO_LARGEUR, PHOTO_HAUTEUR);
            c.clip();
            c.drawImage(image, xPhoto - (l - PHOTO_LARGEUR) / 2, yPhoto - (h - PHOTO_HAUTEUR) / 2, l, h);
            c.restoreGraphicsState();
        } else {
            c.setNonStrokingColor(new Color(0x99, 0x99, 0x99));
            ecrireCentre(c, normal, 7, xPhoto, PHOTO_LARGEUR, yPhoto + PHOTO_HAUTEUR / 2, "PHOTO");
            c.setNonStrokingColor(Color.BLACK);
        }
        rectangle(c, xPhoto, yPhoto, PHOTO_LARGEUR, PHOTO_HAUTEUR);

        float largeurChamp = largeur - 2 * MARGE - PHOTO_LARGEUR - 16;
        float ligne = y;
        ligne = champ(c, gras, normal, MARGE, ligne, largeurChamp, "Nom", d.nom());
        ligne = champ(c, gras, normal, MARGE, ligne, largeurChamp, "Prénom(s)", d.prenom());
        ligne = champ(c, gras, normal, MARGE, ligne, largeurChamp / 2 - 6, "Matricule", d.matricule());
        champ(c, gras, normal, MARGE + largeurChamp / 2 + 6, ligne + HAUTEUR_LIGNE + 6, largeurChamp / 2 - 6,
                "Sexe", sexeLisible(d.sexe()));
        ligne = champ(c, gras, normal, MARGE, ligne, largeurChamp / 2 - 6, "Né(e) le",
                d.dateNaissance() == null ? null : d.dateNaissance().format(OfficialDocumentLayout.JOUR));
        champ(c, gras, normal, MARGE + largeurChamp / 2 + 6, ligne + HAUTEUR_LIGNE + 6, largeurChamp / 2 - 6,
                "Lieu de naissance", null);
        ligne = champ(c, gras, normal, MARGE, ligne, largeurChamp, "Classe", d.classe());
        ligne = champ(c, gras, normal, MARGE, ligne, largeurChamp, "Adresse du domicile", null);

        return Math.min(ligne, yPhoto - 6);
    }

    /**
     * Un champ : libellé, puis la valeur posée sur un filet. Le filet est tracé même quand la
     * valeur est connue — c'est ce qui permet de corriger au stylo une adresse ou une date
     * erronée sans refaire la fiche.
     */
    private float champ(
            PDPageContentStream c, PDType1Font gras, PDType1Font normal, float x, float y, float largeurTotale,
            String libelle, String valeur) throws IOException {
        ecrire(c, gras, 8.5f, x, y, libelle + " :");
        float xValeur = x + largeurTexte(gras, 8.5f, libelle + " :") + 6;
        float finFilet = x + largeurTotale;
        if (rempli(valeur)) {
            ecrire(c, normal, 9.5f, xValeur, y, tronquer(normal, 9.5f, valeur, finFilet - xValeur));
        }
        c.setStrokingColor(new Color(0xAA, 0xAA, 0xAA));
        c.setLineWidth(0.4f);
        c.moveTo(xValeur, y - 3);
        c.lineTo(finFilet, y - 3);
        c.stroke();
        c.setStrokingColor(Color.BLACK);
        c.setLineWidth(0.8f);
        return y - HAUTEUR_LIGNE - 6;
    }

    private float responsables(PDPageContentStream c, Donnees d, float largeur, float y) throws IOException {
        PDType1Font normal = normal();
        PDType1Font gras = gras();
        titreSection(c, "RESPONSABLES LÉGAUX", y);
        y -= 10;

        float largeurUtile = largeur - 2 * MARGE;
        float[] colonnes = {largeurUtile * 0.34f, largeurUtile * 0.16f, largeurUtile * 0.22f, largeurUtile * 0.28f};
        String[] entetes = {"NOM ET PRÉNOM(S)", "LIEN", "TÉLÉPHONE", "ADRESSE ÉLECTRONIQUE"};

        float ligneY = y - HAUTEUR_LIGNE;
        float x = MARGE;
        for (int i = 0; i < colonnes.length; i++) {
            rectangle(c, x, ligneY, colonnes[i], HAUTEUR_LIGNE);
            ecrireCentre(c, gras, 7.5f, x, colonnes[i], ligneY + 5, entetes[i]);
            x += colonnes[i];
        }

        // Deux lignes au minimum : une fiche sans responsable enregistré doit tout de même
        // offrir de quoi en écrire un à la main, c'est le cas d'une inscription au guichet.
        int lignes = Math.max(2, d.responsables().size());
        for (int i = 0; i < lignes; i++) {
            ligneY -= HAUTEUR_LIGNE;
            Responsable r = i < d.responsables().size() ? d.responsables().get(i) : null;
            String[] cellules = r == null
                    ? new String[] {"", "", "", ""}
                    : new String[] {
                        r.nom() + (r.principal() ? " (contact principal)" : ""),
                        r.lien() == null ? "" : r.lien(),
                        r.telephone() == null ? "" : r.telephone(),
                        r.courriel() == null ? "" : r.courriel()};
            float cx = MARGE;
            for (int k = 0; k < colonnes.length; k++) {
                rectangle(c, cx, ligneY, colonnes[k], HAUTEUR_LIGNE);
                ecrire(c, normal, 8, cx + 5, ligneY + 5, tronquer(normal, 8, cellules[k], colonnes[k] - 10));
                cx += colonnes[k];
            }
        }
        return ligneY;
    }

    private float frais(PDPageContentStream c, Donnees d, float largeur, float y) throws IOException {
        PDType1Font normal = normal();
        PDType1Font gras = gras();
        titreSection(c, "FRAIS DE SCOLARITÉ", y);
        y -= 10;

        if (d.frais().isEmpty()) {
            ecrire(c, normal, 8.5f, MARGE, y - HAUTEUR_LIGNE,
                    "Aucun échéancier n'est défini pour cette classe.");
            return y - HAUTEUR_LIGNE - 6;
        }

        float largeurUtile = largeur - 2 * MARGE;
        float[] colonnes = {largeurUtile * 0.56f, largeurUtile * 0.22f, largeurUtile * 0.22f};
        String[] entetes = {"DÉSIGNATION", "ÉCHÉANCE", "MONTANT"};

        float ligneY = y - HAUTEUR_LIGNE;
        float x = MARGE;
        for (int i = 0; i < colonnes.length; i++) {
            rectangle(c, x, ligneY, colonnes[i], HAUTEUR_LIGNE);
            ecrireCentre(c, gras, 7.5f, x, colonnes[i], ligneY + 5, entetes[i]);
            x += colonnes[i];
        }

        long total = 0;
        String devise = d.frais().get(0).devise();
        for (Frais frais : d.frais()) {
            ligneY -= HAUTEUR_LIGNE;
            total += frais.montantCentimes();
            String[] cellules = {
                frais.libelle(),
                frais.echeance() == null ? "-" : frais.echeance().format(OfficialDocumentLayout.JOUR),
                montant(frais.montantCentimes(), frais.devise()),
            };
            float cx = MARGE;
            for (int i = 0; i < colonnes.length; i++) {
                rectangle(c, cx, ligneY, colonnes[i], HAUTEUR_LIGNE);
                if (i == 0) {
                    ecrire(c, normal, 8.5f, cx + 5, ligneY + 5,
                            tronquer(normal, 8.5f, cellules[i], colonnes[i] - 10));
                } else {
                    ecrireCentre(c, normal, 8.5f, cx, colonnes[i], ligneY + 5, cellules[i]);
                }
                cx += colonnes[i];
            }
        }

        ligneY -= HAUTEUR_LIGNE;
        rectangle(c, MARGE, ligneY, colonnes[0] + colonnes[1], HAUTEUR_LIGNE);
        ecrire(c, gras, 8.5f, MARGE + 5, ligneY + 5, "TOTAL DÛ POUR L'ANNÉE");
        rectangle(c, MARGE + colonnes[0] + colonnes[1], ligneY, colonnes[2], HAUTEUR_LIGNE);
        ecrireCentre(c, gras, 9, MARGE + colonnes[0] + colonnes[1], colonnes[2], ligneY + 5,
                montant(total, devise));
        return ligneY;
    }

    private float engagement(PDPageContentStream c, float largeur, float y) throws IOException {
        PDType1Font normal = normal();
        titreSection(c, "ENGAGEMENT DU RESPONSABLE LÉGAL", y);
        y -= 10;
        String[] texte = {
            "Je soussigné(e), responsable légal de l'élève désigné ci-dessus, demande son inscription",
            "pour l'année scolaire indiquée. Je déclare exactes les informations portées sur cette fiche,",
            "m'engage à respecter le règlement intérieur de l'établissement et à m'acquitter des frais",
            "de scolarité aux échéances convenues.",
        };
        for (String phrase : texte) {
            y -= HAUTEUR_LIGNE - 4;
            ecrire(c, normal, 8.5f, MARGE, y, phrase);
        }
        return y - 6;
    }

    private void signatures(PDPageContentStream c, Tenant tenant, float largeur, float y) throws IOException {
        PDType1Font gras = gras();
        ecrire(c, gras, 9, MARGE, y, "Le responsable légal");
        String fonction = "Le Directeur";
        ecrire(c, gras, 9, largeur - MARGE - largeurTexte(gras, 9, fonction), y, fonction);
        if (rempli(tenant.getDirectorName())) {
            String nom = tenant.getDirectorName();
            ecrire(c, gras, 9, largeur - MARGE - largeurTexte(gras, 9, nom), y - HAUTEUR_LIGNE * 3, nom);
        }
    }

    private void titreSection(PDPageContentStream c, String titre, float y) throws IOException {
        ecrire(c, gras(), 9.5f, MARGE, y, titre);
    }

    private String sexeLisible(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toUpperCase(Locale.FRENCH)) {
            case "F" -> "Féminin";
            case "M" -> "Masculin";
            default -> code;
        };
    }

    /** Montant affiché avec le libellé lisible de la devise : XOF devient « FCFA ». */
    private String montant(long centimes, String devise) {
        return MoneyFormat.format(centimes, devise);
    }
}
