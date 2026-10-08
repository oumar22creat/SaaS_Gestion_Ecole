package com.schoolsaas.officialdoc;

import com.schoolsaas.common.TexteImprimable;
import com.schoolsaas.tenant.Tenant;
import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * En-tête, signature et pied de page communs aux documents officiels de l'établissement :
 * bulletin, rapport trimestriel, certificat de scolarité.
 *
 * <p>Ces pièces sortent du même secrétariat et sont présentées aux mêmes tiers — une
 * famille, une autre école, l'inspection, le ministère. Elles doivent se ressembler, et
 * surtout porter les mêmes mentions : si l'académie change sur le bulletin et pas sur le
 * rapport, c'est l'établissement qui a l'air de ne pas savoir de qui il relève.
 *
 * <p>Tout est saisi par l'établissement ({@link Tenant}) et rien n'est codé en dur. Le
 * découpage administratif n'est pas le même d'un pays à l'autre — académie et inspection au
 * Sénégal, académie d'enseignement et CAP au Mali — et une liste fermée ne servirait qu'un
 * seul système scolaire. Un établissement qui n'a rien renseigné obtient un document
 * correct, simplement sans ces lignes.
 */
public final class OfficialDocumentLayout {

    public static final float MARGE = 40;
    public static final float HAUTEUR_LIGNE = 16;
    public static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final float LOGO_MAX = 54;

    private OfficialDocumentLayout() {
    }

    public static PDType1Font normal() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    public static PDType1Font gras() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }

    /**
     * En-tête administratif : État, académie, inspection et nom de l'établissement à gauche ;
     * logo et raison sociale à droite. Renvoie l'ordonnée où le corps peut commencer.
     *
     * <p>Le point de départ du corps est calculé, jamais fixé d'avance : un établissement
     * avec trois mentions officielles et un logo haut verrait sinon son titre se poser
     * par-dessus son en-tête.
     */
    public static float enTete(
            PDDocument document, PDPageContentStream c, Tenant tenant, byte[] logo, float largeur, float hauteur)
            throws IOException {
        PDType1Font normal = normal();
        PDType1Font gras = gras();
        float haut = hauteur - MARGE;
        float basLogo = haut;

        if (logo != null) {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, logo, "logo");
            float echelle = Math.min(LOGO_MAX / image.getWidth(), LOGO_MAX / image.getHeight());
            float l = image.getWidth() * echelle;
            float h = image.getHeight() * echelle;
            basLogo = haut - h;
            c.drawImage(image, largeur - MARGE - l, basLogo, l, h);
        }

        float ligne = haut - 12;
        if (rempli(tenant.getOfficialAuthority())) {
            ecrire(c, gras, 13, MARGE, ligne, tenant.getOfficialAuthority());
            ligne -= HAUTEUR_LIGNE;
        }
        for (String mention : new String[] {tenant.getAcademyLabel(), tenant.getInspectionLabel()}) {
            if (rempli(mention)) {
                ecrire(c, gras, 9, MARGE, ligne, mention);
                ligne -= HAUTEUR_LIGNE - 3;
            }
        }
        // Le nom de l'établissement ferme le bloc administratif : c'est lui qui délivre le
        // document, les lignes au-dessus disent seulement de qui il relève.
        ecrire(c, gras, 9, MARGE, ligne, tenant.getName().toUpperCase(Locale.FRENCH));
        ligne -= HAUTEUR_LIGNE - 3;

        float basColonneDroite = basLogo;
        if (logo != null) {
            c.setNonStrokingColor(couleur(tenant.getPrimaryColor()));
            String nom = tenant.getName().toUpperCase(Locale.FRENCH);
            basColonneDroite = basLogo - 16;
            ecrire(c, gras, 13, largeur - MARGE - largeurTexte(gras, 13, nom), basColonneDroite, nom);
            c.setNonStrokingColor(Color.BLACK);
        }
        // Coordonnées de l'école sous le bloc administratif : un document officiel doit dire
        // où joindre celui qui l'a émis, sans quoi le destinataire n'a que le pied de page.
        if (rempli(tenant.getPostalAddress())) {
            ecrire(c, normal, 7.5f, MARGE, ligne, tenant.getPostalAddress());
            ligne -= HAUTEUR_LIGNE - 5;
        }

        return Math.min(ligne, basColonneDroite) - 14;
    }

    /** Titre centré et sous-titre, renvoie l'ordonnée suivante. */
    public static float titre(PDPageContentStream c, String titre, String sousTitre, float largeur, float y)
            throws IOException {
        PDType1Font gras = gras();
        PDType1Font normal = normal();
        ecrire(c, gras, 15, centre(gras, 15, titre, largeur), y, titre);
        if (rempli(sousTitre)) {
            ecrire(c, normal, 11, centre(normal, 11, sousTitre, largeur), y - HAUTEUR_LIGNE, sousTitre);
            return y - HAUTEUR_LIGNE * 2 - 8;
        }
        return y - HAUTEUR_LIGNE - 8;
    }

    /**
     * Bloc de signature : la fonction, trois interlignes de blanc pour la signature
     * manuscrite, puis le nom du directeur.
     *
     * <p>C'est le nom saisi par l'établissement, pas un intitulé générique : un document
     * qui ne nomme pas son signataire n'engage personne, et l'inspection le renvoie.
     */
    public static void signature(PDPageContentStream c, Tenant tenant, float largeur, float y) throws IOException {
        PDType1Font gras = gras();
        String fonction = "LE DIRECTEUR";
        ecrire(c, gras, 10, largeur - MARGE - largeurTexte(gras, 10, fonction), y, fonction);
        if (rempli(tenant.getDirectorName())) {
            String nom = tenant.getDirectorName();
            ecrire(c, gras, 10, largeur - MARGE - largeurTexte(gras, 10, nom), y - HAUTEUR_LIGNE * 3, nom);
        }
    }

    /** Ville et date à gauche, mentions légales en dessous. */
    public static void piedDePage(PDPageContentStream c, Tenant tenant, float largeur) throws IOException {
        PDType1Font normal = normal();
        String ville = rempli(tenant.getHeadOfficeCity()) ? tenant.getHeadOfficeCity() : "";
        String date = (ville.isEmpty() ? "Le " : ville + ", le ") + LocalDate.now().format(JOUR);
        ecrire(c, normal, 8, MARGE, MARGE, date);
        if (rempli(tenant.getReportCardLegalMentions())) {
            ecrire(c, normal, 7,
                    largeur - MARGE - largeurTexte(normal, 7, tenant.getReportCardLegalMentions()), MARGE,
                    tenant.getReportCardLegalMentions());
        }
    }

    // ------------------------------------------------------------------ primitives de dessin

    public static void rectangle(PDPageContentStream c, float x, float y, float largeur, float hauteur)
            throws IOException {
        c.addRect(x, y, largeur, hauteur);
        c.stroke();
    }

    public static void ecrire(
            PDPageContentStream c, PDType1Font police, float taille, float x, float y, String texte)
            throws IOException {
        c.beginText();
        c.setFont(police, taille);
        c.newLineAtOffset(x, y);
        c.showText(TexteImprimable.nettoyer(texte));
        c.endText();
    }

    public static void ecrireCentre(
            PDPageContentStream c, PDType1Font police, float taille, float x, float largeurCellule, float y,
            String texte) throws IOException {
        String propre = TexteImprimable.nettoyer(texte);
        ecrire(c, police, taille, x + (largeurCellule - largeurTexte(police, taille, propre)) / 2, y, propre);
    }

    public static float centre(PDType1Font police, float taille, String texte, float largeurPage) {
        return (largeurPage - largeurTexte(police, taille, texte)) / 2;
    }

    public static float largeurTexte(PDType1Font police, float taille, String texte) {
        try {
            return police.getStringWidth(TexteImprimable.nettoyer(texte)) / 1000 * taille;
        } catch (IOException e) {
            // Mesure impossible : mieux vaut un texte mal centré qu'un document non produit.
            return texte.length() * taille * 0.5f;
        }
    }

    public static String tronquer(PDType1Font police, float taille, String texte, float largeurDisponible) {
        String propre = TexteImprimable.nettoyer(texte);
        if (largeurTexte(police, taille, propre) <= largeurDisponible - 6) {
            return propre;
        }
        StringBuilder court = new StringBuilder();
        for (char caractere : propre.toCharArray()) {
            if (largeurTexte(police, taille, court.toString() + caractere + "...") > largeurDisponible - 6) {
                break;
            }
            court.append(caractere);
        }
        return court + "...";
    }

    public static boolean rempli(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    /** Une couleur mal saisie ne doit pas empêcher d'imprimer : on retombe sur celle du produit. */
    public static Color couleur(String hex) {
        if (hex == null || !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            return new Color(0x0f, 0x5c, 0x4c);
        }
        return new Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }
}
