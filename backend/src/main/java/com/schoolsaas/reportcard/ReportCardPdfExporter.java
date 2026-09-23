package com.schoolsaas.reportcard;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.paperwork.SchoolBrandedPdf;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.student.Student;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.tenant.Tenant;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Export PDF d'un bulletin (cahier-des-charges.md §12) — mise en page simple. En-tête et
 * mentions légales personnalisables par établissement (cahier §2.4, ROADMAP.md 3.7,
 * {@code Tenant#reportCardHeader}/{@code #reportCardLegalMentions}) ; à défaut, un en-tête
 * générique et pas de mentions légales — comportement inchangé pour un tenant qui n'a jamais
 * configuré son modèle de bulletin.
 */
@Component
public class ReportCardPdfExporter {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 18;

    public byte[] export(
            ReportCard reportCard, List<ReportCardEntry> entries, Student student, SchoolClass schoolClass,
            Map<Long, Subject> subjectsById, Tenant tenant, byte[] logo) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // En-tête commun aux trois documents officiels : un bulletin, un certificat et
                // un reçu du même établissement doivent se ressembler.
                float y = SchoolBrandedPdf.drawHeader(
                        document, content, tenant,
                        logo,
                        "BULLETIN SCOLAIRE — " + reportCard.getPeriodLabel().toUpperCase(java.util.Locale.FRENCH),
                        page.getMediaBox().getWidth(),
                        page.getMediaBox().getHeight());
                y = writeLine(content, regular, 11, MARGIN, y,
                        "Élève : " + student.getFirstName() + " " + student.getLastName() + " (" + student.getStudentNumber() + ")");
                y = writeLine(content, regular, 11, MARGIN, y, "Classe : " + schoolClass.getName());
                y -= LINE_HEIGHT;

                y = writeLine(content, bold, 12, MARGIN, y, "Matière");
                writeLine(content, bold, 12, MARGIN + 250, y + LINE_HEIGHT, "Moyenne /20");
                writeLine(content, bold, 12, MARGIN + 350, y + LINE_HEIGHT, "Coeff.");
                y -= LINE_HEIGHT / 2;

                for (ReportCardEntry entry : entries) {
                    String subjectName = subjectsById.containsKey(entry.getSubjectId())
                            ? subjectsById.get(entry.getSubjectId()).getName()
                            : "Matière #" + entry.getSubjectId();
                    y = writeLine(content, regular, 11, MARGIN, y, subjectName);
                    writeLine(content, regular, 11, MARGIN + 250, y + LINE_HEIGHT, entry.getAverage() == null ? "—" : String.valueOf(entry.getAverage()));
                    writeLine(content, regular, 11, MARGIN + 350, y + LINE_HEIGHT, String.valueOf(entry.getCoefficient()));
                    if (entry.getTeacherComment() != null) {
                        y = writeLine(content, regular, 9, MARGIN + 20, y, entry.getTeacherComment());
                    }
                }

                y -= LINE_HEIGHT / 2;
                y = writeLine(content, bold, 12, MARGIN, y,
                        "Moyenne générale : " + (reportCard.getGeneralAverage() == null ? "—" : reportCard.getGeneralAverage()) + "/20");
                // Le rang est la première ligne que cherche un parent sur un bulletin.
                y = writeLine(content, bold, 12, MARGIN, y, "Rang : " + formatRank(reportCard));
                y = writeLine(content, regular, 11, MARGIN, y,
                        "Absences : " + reportCard.getAbsenceCount() + " · Retards : " + reportCard.getLateCount());

                // Un libellé suivi de rien fait paraître le document inachevé : une
                // appréciation vide vaut mieux absente qu'annoncée.
                if (isFilled(reportCard.getGeneralComment())) {
                    y -= LINE_HEIGHT / 2;
                    y = writeLine(content, bold, 11, MARGIN, y, "Appréciation générale :");
                    y = writeLine(content, regular, 11, MARGIN, y, reportCard.getGeneralComment());
                }
                if (isFilled(reportCard.getCouncilDecision())) {
                    y -= LINE_HEIGHT / 2;
                    y = writeLine(content, bold, 11, MARGIN, y, "Décision du conseil de classe :");
                    y = writeLine(content, regular, 11, MARGIN, y, reportCard.getCouncilDecision());
                }
                SchoolBrandedPdf.drawLegalMentions(content, tenant);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw ApiException.unprocessable("PDF_GENERATION_FAILED", "Impossible de générer le PDF du bulletin");
        }
    }

    /**
     * « 3e sur 42 ». Un élève sans moyenne n'est pas classé : le dire explicitement évite de
     * laisser croire à une erreur de calcul.
     */
    private static boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private static String formatRank(ReportCard reportCard) {
        if (reportCard.getRankInClass() == null || reportCard.getClassSize() == null) {
            return "non classé";
        }
        int rank = reportCard.getRankInClass();
        return (rank == 1 ? "1er" : rank + "e") + " sur " + reportCard.getClassSize();
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, float fontSize, float x, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - LINE_HEIGHT;
    }
}
