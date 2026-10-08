package com.schoolsaas.idcard;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.TexteImprimable;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.student.Student;
import com.schoolsaas.tenant.Tenant;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
 * Cartes d'identité scolaires, au format carte bancaire (85,6 × 54 mm), huit par page A4.
 *
 * <p>Huit par page et non une : une carte seule gâche une feuille, et une classe de
 * cinquante élèves en consommerait cinquante. Le secrétariat imprime la planche sur du
 * papier épais, découpe suivant les traits, et plastifie. Les repères de coupe sont tracés
 * pour cela — sans eux, les cartes sortent de travers et le portrait se retrouve coupé.
 *
 * <p>Un élève sans photo obtient quand même sa carte, avec un cadre vide barré d'une
 * mention : bloquer l'impression de toute une classe parce que trois portraits manquent
 * obligerait le secrétariat à faire le tri à la main avant chaque tirage.
 */
@Component
public class StudentIdCardPdfExporter {

    /** 85,6 × 54 mm en points PostScript — le format ISO/IEC 7810 ID-1, celui des plastifieuses. */
    private static final float LARGEUR_CARTE = 242.6f;
    private static final float HAUTEUR_CARTE = 153.0f;

    private static final int COLONNES = 2;
    private static final int LIGNES = 4;
    private static final float ECART = 14;

    private static final float BANDEAU = 26;
    private static final float PHOTO_LARGEUR = 62;
    private static final float PHOTO_HAUTEUR = 78;

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] export(
            List<Student> eleves, SchoolClass classe, Map<Long, byte[]> portraits, String anneeScolaire,
            Tenant tenant, byte[] logo) {
        if (eleves.isEmpty()) {
            throw ApiException.unprocessable(
                    "NO_STUDENT_TO_PRINT", "Aucun élève dans cette classe : il n'y a pas de carte à produire");
        }
        try (PDDocument document = new PDDocument()) {
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            Color couleurEcole = couleur(tenant.getPrimaryColor());

            int parPage = COLONNES * LIGNES;
            for (int debut = 0; debut < eleves.size(); debut += parPage) {
                List<Student> page = eleves.subList(debut, Math.min(debut + parPage, eleves.size()));
                dessinerPlanche(document, page, classe, portraits, anneeScolaire, tenant, logo, normal, gras,
                        couleurEcole);
            }

            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw ApiException.unprocessable("PDF_GENERATION_FAILED", "Impossible de générer les cartes d'élèves");
        }
    }

    private void dessinerPlanche(
            PDDocument document, List<Student> eleves, SchoolClass classe, Map<Long, byte[]> portraits,
            String anneeScolaire, Tenant tenant, byte[] logo, PDType1Font normal, PDType1Font gras, Color couleurEcole)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        float largeurPage = page.getMediaBox().getWidth();
        float hauteurPage = page.getMediaBox().getHeight();

        float largeurGrille = COLONNES * LARGEUR_CARTE + (COLONNES - 1) * ECART;
        float hauteurGrille = LIGNES * HAUTEUR_CARTE + (LIGNES - 1) * ECART;
        float gaucheGrille = (largeurPage - largeurGrille) / 2;
        float hautGrille = (hauteurPage + hauteurGrille) / 2;

        PDImageXObject imageLogo =
                logo == null ? null : PDImageXObject.createFromByteArray(document, logo, "logo");

        try (PDPageContentStream c = new PDPageContentStream(document, page)) {
            for (int index = 0; index < eleves.size(); index++) {
                float x = gaucheGrille + (index % COLONNES) * (LARGEUR_CARTE + ECART);
                float y = hautGrille - (index / COLONNES + 1) * HAUTEUR_CARTE - (index / COLONNES) * ECART;
                byte[] portrait = portraits.get(eleves.get(index).getId());
                PDImageXObject imagePortrait = portrait == null
                        ? null
                        : PDImageXObject.createFromByteArray(document, portrait, "portrait");
                dessinerCarte(c, x, y, eleves.get(index), classe, imagePortrait, anneeScolaire, tenant, imageLogo,
                        normal, gras, couleurEcole);
            }
        }
    }

    private void dessinerCarte(
            PDPageContentStream c, float x, float y, Student eleve, SchoolClass classe, PDImageXObject portrait,
            String anneeScolaire, Tenant tenant, PDImageXObject logo, PDType1Font normal, PDType1Font gras,
            Color couleurEcole) throws IOException {
        // Contour : c'est le trait de coupe autant que le cadre de la carte.
        c.setStrokingColor(new Color(0x99, 0x99, 0x99));
        c.setLineWidth(0.5f);
        c.addRect(x, y, LARGEUR_CARTE, HAUTEUR_CARTE);
        c.stroke();

        // Bandeau d'en-tête aux couleurs de l'école.
        c.setNonStrokingColor(couleurEcole);
        c.addRect(x, y + HAUTEUR_CARTE - BANDEAU, LARGEUR_CARTE, BANDEAU);
        c.fill();

        float xTexteEnTete = x + 8;
        if (logo != null) {
            float hauteurLogo = BANDEAU - 8;
            float largeurLogo = logo.getWidth() * (hauteurLogo / logo.getHeight());
            largeurLogo = Math.min(largeurLogo, 34);
            c.drawImage(logo, x + 6, y + HAUTEUR_CARTE - BANDEAU + 4, largeurLogo, hauteurLogo);
            xTexteEnTete = x + 6 + largeurLogo + 6;
        }
        c.setNonStrokingColor(Color.WHITE);
        ecrire(c, gras, 8.5f, xTexteEnTete,
                y + HAUTEUR_CARTE - BANDEAU + 14, tronquer(gras, 8.5f, tenant.getName().toUpperCase(Locale.FRENCH),
                        LARGEUR_CARTE - (xTexteEnTete - x) - 8));
        ecrire(c, normal, 6.5f, xTexteEnTete, y + HAUTEUR_CARTE - BANDEAU + 5, "CARTE D'ÉLÈVE");
        c.setNonStrokingColor(Color.BLACK);

        // Portrait, calé à gauche sous le bandeau.
        float xPhoto = x + 10;
        float yPhoto = y + HAUTEUR_CARTE - BANDEAU - PHOTO_HAUTEUR - 8;
        if (portrait != null) {
            // Recadrage par le centre : une photo panoramique posée telle quelle écraserait
            // le visage, et c'est le visage qui sert à l'identification.
            float echelle = Math.max(PHOTO_LARGEUR / portrait.getWidth(), PHOTO_HAUTEUR / portrait.getHeight());
            float largeur = portrait.getWidth() * echelle;
            float hauteur = portrait.getHeight() * echelle;
            c.saveGraphicsState();
            c.addRect(xPhoto, yPhoto, PHOTO_LARGEUR, PHOTO_HAUTEUR);
            c.clip();
            c.drawImage(portrait, xPhoto - (largeur - PHOTO_LARGEUR) / 2, yPhoto - (hauteur - PHOTO_HAUTEUR) / 2,
                    largeur, hauteur);
            c.restoreGraphicsState();
        } else {
            c.setNonStrokingColor(new Color(0xF0, 0xF0, 0xF0));
            c.addRect(xPhoto, yPhoto, PHOTO_LARGEUR, PHOTO_HAUTEUR);
            c.fill();
            c.setNonStrokingColor(new Color(0x88, 0x88, 0x88));
            ecrireCentre(c, normal, 6, xPhoto, PHOTO_LARGEUR, yPhoto + PHOTO_HAUTEUR / 2, "PHOTO");
            ecrireCentre(c, normal, 6, xPhoto, PHOTO_LARGEUR, yPhoto + PHOTO_HAUTEUR / 2 - 9, "MANQUANTE");
            c.setNonStrokingColor(Color.BLACK);
        }
        c.setStrokingColor(new Color(0xBB, 0xBB, 0xBB));
        c.addRect(xPhoto, yPhoto, PHOTO_LARGEUR, PHOTO_HAUTEUR);
        c.stroke();

        // Identité, à droite du portrait.
        float xInfos = xPhoto + PHOTO_LARGEUR + 10;
        float largeurInfos = LARGEUR_CARTE - (xInfos - x) - 10;
        float ligne = y + HAUTEUR_CARTE - BANDEAU - 14;

        ecrire(c, gras, 10, xInfos, ligne,
                tronquer(gras, 10, eleve.getLastName().toUpperCase(Locale.FRENCH), largeurInfos));
        ligne -= 12;
        ecrire(c, normal, 9.5f, xInfos, ligne, tronquer(normal, 9.5f, eleve.getFirstName(), largeurInfos));
        ligne -= 16;

        ligne = champ(c, normal, gras, xInfos, ligne, largeurInfos, "Matricule", eleve.getStudentNumber());
        ligne = champ(c, normal, gras, xInfos, ligne, largeurInfos, "Classe", classe.getName());
        // L'année n'est pas répétée ici : elle figure déjà en pied de carte, et un quatrième
        // champ venait buter dans la ligne de séparation.
        champ(c, normal, gras, xInfos, ligne, largeurInfos, "Né(e) le",
                eleve.getBirthDate() == null ? "-" : eleve.getBirthDate().format(JOUR));

        // Pied : la carte doit dire qui l'a émise et jusqu'à quand elle vaut.
        c.setStrokingColor(new Color(0xDD, 0xDD, 0xDD));
        c.moveTo(x + 10, y + 20);
        c.lineTo(x + LARGEUR_CARTE - 10, y + 20);
        c.stroke();
        c.setNonStrokingColor(new Color(0x66, 0x66, 0x66));
        ecrire(c, normal, 6, x + 10, y + 10,
                tronquer(normal, 6, "Valable pour l'année scolaire " + anneeScolaire, LARGEUR_CARTE - 100));
        if (tenant.getDirectorName() != null && !tenant.getDirectorName().isBlank()) {
            String signature = "Le Directeur : " + tenant.getDirectorName();
            ecrire(c, normal, 6, x + LARGEUR_CARTE - 10 - largeurTexte(normal, 6, signature), y + 10, signature);
        }
        c.setNonStrokingColor(Color.BLACK);
        c.setStrokingColor(Color.BLACK);
    }

    private float champ(
            PDPageContentStream c, PDType1Font normal, PDType1Font gras, float x, float y, float largeurDisponible,
            String libelle, String valeur) throws IOException {
        c.setNonStrokingColor(new Color(0x77, 0x77, 0x77));
        ecrire(c, normal, 6, x, y, libelle.toUpperCase(Locale.FRENCH));
        c.setNonStrokingColor(Color.BLACK);
        ecrire(c, gras, 8, x, y - 9, tronquer(gras, 8, valeur == null ? "—" : valeur, largeurDisponible));
        return y - 22;
    }

    // ------------------------------------------------------------------ primitives de dessin

    private void ecrire(PDPageContentStream c, PDType1Font police, float taille, float x, float y, String texte)
            throws IOException {
        c.beginText();
        c.setFont(police, taille);
        c.newLineAtOffset(x, y);
        c.showText(TexteImprimable.nettoyer(texte));
        c.endText();
    }

    private void ecrireCentre(
            PDPageContentStream c, PDType1Font police, float taille, float x, float largeurCellule, float y,
            String texte) throws IOException {
        String propre = TexteImprimable.nettoyer(texte);
        ecrire(c, police, taille, x + (largeurCellule - largeurTexte(police, taille, propre)) / 2, y, propre);
    }

    private float largeurTexte(PDType1Font police, float taille, String texte) {
        try {
            return police.getStringWidth(TexteImprimable.nettoyer(texte)) / 1000 * taille;
        } catch (IOException e) {
            return texte.length() * taille * 0.5f;
        }
    }

    private String tronquer(PDType1Font police, float taille, String texte, float largeurDisponible) {
        String propre = TexteImprimable.nettoyer(texte);
        if (largeurTexte(police, taille, propre) <= largeurDisponible) {
            return propre;
        }
        StringBuilder court = new StringBuilder();
        for (char caractere : propre.toCharArray()) {
            if (largeurTexte(police, taille, court.toString() + caractere + ".") > largeurDisponible) {
                break;
            }
            court.append(caractere);
        }
        return court + ".";
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
