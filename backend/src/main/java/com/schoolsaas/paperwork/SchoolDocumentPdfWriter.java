package com.schoolsaas.paperwork;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.Tenant;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Mise en page commune des documents officiels remis au guichet — certificat de scolarité,
 * reçu de règlement (cahier-des-charges.md §7/§19.4).
 *
 * <p>Un même en-tête d'établissement, un même pied de page : ces papiers sortent du même
 * secrétariat et doivent se ressembler. La mise en forme est délibérément sobre — ils sont
 * imprimés en noir et blanc, souvent sur du papier déjà à en-tête.
 */
@Component
public class SchoolDocumentPdfWriter {

    public static final DateTimeFormatter FRENCH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 18;

    /**
     * @param title      titre du document, en haut sous l'en-tête
     * @param paragraphs corps du document, une entrée par ligne ; une entrée vide saute une ligne
     * @param footer     mention finale (lieu et date, signature), ou null
     */
    public byte[] write(Tenant tenant, String title, List<String> paragraphs, String footer) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                String header = tenant.getReportCardHeader() != null ? tenant.getReportCardHeader() : tenant.getName();
                y = writeLine(content, bold, 16, y, header);
                y -= LINE_HEIGHT / 2;
                y = writeLine(content, bold, 14, y, title);
                y -= LINE_HEIGHT;

                for (String paragraph : paragraphs) {
                    if (paragraph.isEmpty()) {
                        y -= LINE_HEIGHT / 2;
                        continue;
                    }
                    y = writeLine(content, regular, 11, y, paragraph);
                }

                if (footer != null) {
                    y -= LINE_HEIGHT * 2;
                    y = writeLine(content, regular, 11, y, footer);
                }
                if (tenant.getReportCardLegalMentions() != null) {
                    writeLine(content, regular, 8, MARGIN, tenant.getReportCardLegalMentions());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw ApiException.unprocessable("PDF_GENERATION_FAILED", "Impossible de générer le document PDF");
        }
    }

    /** « Fait à Bamako, le 22/09/2026 » — la formule attendue au bas d'un document officiel. */
    public static String issuedAt(String place, LocalDate date) {
        return "Fait à " + place + ", le " + date.format(FRENCH_DATE);
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, float fontSize, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(MARGIN, y);
        content.showText(text);
        content.endText();
        return y - LINE_HEIGHT;
    }
}
