package com.schoolsaas.paperwork;

import com.schoolsaas.tenant.Tenant;
import java.awt.Color;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * En-tête et pied de page communs à tous les documents officiels — bulletin, certificat de
 * scolarité, reçu de règlement (cahier-des-charges.md §2.4).
 *
 * <p>Un même établissement, une même identité : ces papiers sortent du même secrétariat et
 * sont présentés à des tiers — une banque, une autre école, une administration. Logo,
 * couleurs et mentions légales sont donc posés au même endroit pour les trois, plutôt que
 * réécrits dans chaque exporteur où ils auraient fini par diverger.
 */
public final class SchoolBrandedPdf {

    public static final float MARGIN = 50;
    public static final float LINE_HEIGHT = 18;

    /** Hauteur maximale du logo : au-delà, il mangerait la première ligne du document. */
    private static final float LOGO_MAX_HEIGHT = 46;
    private static final float LOGO_MAX_WIDTH = 120;

    private SchoolBrandedPdf() {
    }

    /**
     * Dessine l'en-tête et renvoie l'ordonnée où le corps du document peut commencer.
     *
     * @param logo contenu du logo, ou null — un établissement qui n'en a pas doit obtenir un
     *             document correct, pas un trou dans la mise en page
     */
    public static float drawHeader(
            PDDocument document, PDPageContentStream content, Tenant tenant, byte[] logo, String title,
            float pageWidth, float pageHeight) throws IOException {
        float top = pageHeight - MARGIN;
        float textLeft = MARGIN;
        float logoBottom = top;

        if (logo != null) {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, logo, "logo");
            float scale = Math.min(LOGO_MAX_WIDTH / image.getWidth(), LOGO_MAX_HEIGHT / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            logoBottom = top - height;
            content.drawImage(image, MARGIN, logoBottom, width, height);
            // Le texte se décale à droite du logo : superposés, ni l'un ni l'autre ne serait
            // lisible sur une impression en noir et blanc.
            textLeft = MARGIN + width + 14;
        }

        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        // Première ligne calée sous le haut de page, comme le sommet du logo.
        float textBaseline = top - 14;
        String header = tenant.getReportCardHeader() != null ? tenant.getReportCardHeader() : tenant.getName();
        writeAt(content, bold, 15, textLeft, textBaseline, header);
        if (tenant.getReportCardHeader() != null && !tenant.getReportCardHeader().equals(tenant.getName())) {
            // L'en-tête personnalisé peut être une raison sociale complète ; le nom usuel
            // reste utile pour identifier l'établissement d'un coup d'œil.
            textBaseline -= LINE_HEIGHT;
            writeAt(content, regular, 10, textLeft, textBaseline, tenant.getName());
        }

        // Le filet passe sous le plus bas des deux blocs. Calculé et non fixé d'avance : avec
        // une hauteur figée, il traversait le logo des établissements au logo haut.
        float ruleY = Math.min(logoBottom, textBaseline - 6) - 10;
        content.setStrokingColor(colorOf(tenant.getPrimaryColor(), new Color(0x0f, 0x5c, 0x4c)));
        content.setLineWidth(2);
        content.moveTo(MARGIN, ruleY);
        content.lineTo(pageWidth - MARGIN, ruleY);
        content.stroke();
        content.setStrokingColor(Color.BLACK);

        float titleBaseline = ruleY - LINE_HEIGHT - 6;
        writeAt(content, bold, 14, MARGIN, titleBaseline, title);
        return titleBaseline - LINE_HEIGHT - 6;
    }

    /** Mentions légales en bas de page, si l'établissement en a configuré. */
    public static void drawLegalMentions(PDPageContentStream content, Tenant tenant) throws IOException {
        if (tenant.getReportCardLegalMentions() == null || tenant.getReportCardLegalMentions().isBlank()) {
            return;
        }
        writeAt(content, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8, MARGIN, MARGIN,
                tenant.getReportCardLegalMentions());
    }

    public static void writeAt(
            PDPageContentStream content, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    /** Une couleur mal saisie ne doit pas empêcher d'imprimer : on retombe sur celle du produit. */
    private static Color colorOf(String hex, Color fallback) {
        if (hex == null || !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            return fallback;
        }
        return new Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }
}
