package com.schoolsaas.termreport;

import com.schoolsaas.attendance.AttendanceRecord;
import com.schoolsaas.attendance.AttendanceRecordRepository;
import com.schoolsaas.attendance.AttendanceStatus;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.NumberUtils;
import com.schoolsaas.discipline.Incident;
import com.schoolsaas.discipline.IncidentRepository;
import com.schoolsaas.discipline.Observation;
import com.schoolsaas.discipline.ObservationRepository;
import com.schoolsaas.discipline.SanctionRepository;
import com.schoolsaas.reportcard.ReportCard;
import com.schoolsaas.reportcard.ReportCardEntry;
import com.schoolsaas.reportcard.ReportCardService;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassService;
import com.schoolsaas.schoolyear.SchoolYear;
import com.schoolsaas.schoolyear.SchoolYearService;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Rapport trimestriel d'une classe, destiné à la tutelle (cahier-des-charges.md §12).
 *
 * <p>Il est calculé à partir des bulletins déjà générés, et pas des notes brutes. Ce n'est
 * pas un détour : un rapport qui ne dirait pas la même chose que les bulletins remis aux
 * familles le même trimestre serait indéfendable devant l'inspection. Les bulletins sont la
 * source de vérité une fois figés ; le rapport les agrège.
 *
 * <p>Conséquence assumée : sans bulletins générés, pas de rapport. Le message le dit, plutôt
 * que de rendre un document vide que le directeur enverrait sans le relire.
 */
@Service
public class TermReportService {

    /** Les tranches que lisent les inspections : le seuil de 10 au milieu, puis par deux. */
    private static final double[] BORNES = {0, 5, 8, 10, 12, 14, 16, 20.01};

    private final ReportCardService reportCardService;
    private final SchoolClassService schoolClassService;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final IncidentRepository incidentRepository;
    private final SanctionRepository sanctionRepository;
    private final ObservationRepository observationRepository;
    private final SchoolYearService schoolYearService;

    public TermReportService(
            ReportCardService reportCardService,
            SchoolClassService schoolClassService,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            AttendanceRecordRepository attendanceRepository,
            IncidentRepository incidentRepository,
            SanctionRepository sanctionRepository,
            ObservationRepository observationRepository,
            SchoolYearService schoolYearService) {
        this.reportCardService = reportCardService;
        this.schoolClassService = schoolClassService;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.attendanceRepository = attendanceRepository;
        this.incidentRepository = incidentRepository;
        this.sanctionRepository = sanctionRepository;
        this.observationRepository = observationRepository;
        this.schoolYearService = schoolYearService;
    }

    public TermReport build(Long schoolClassId, String periodLabel) {
        SchoolClass classe = schoolClassService.getById(schoolClassId);
        List<ReportCard> bulletins = reportCardService.listForClassAndPeriod(schoolClassId, periodLabel);
        if (bulletins.isEmpty()) {
            throw ApiException.unprocessable(
                    "NO_REPORT_CARD_FOR_PERIOD",
                    "Aucun bulletin généré pour cette classe et cette période. Le rapport reprend les "
                            + "bulletins : générez-les d'abord, pour que les deux documents concordent.");
        }

        ReportCard premier = bulletins.get(0);
        List<Student> eleves = studentRepository.findAllBySchoolClassId(schoolClassId).stream()
                .filter(Student::isActive)
                .toList();

        return new TermReport(
                classe.getName(),
                nomDuTitulaire(classe),
                periodLabel,
                anneeScolaire(premier),
                premier.getPeriodFrom(),
                premier.getPeriodTo(),
                effectif(eleves, bulletins),
                resultats(bulletins),
                matieres(bulletins),
                repartition(bulletins),
                assiduite(schoolClassId, premier, eleves.size()),
                discipline(schoolClassId, premier, eleves));
    }

    private String nomDuTitulaire(SchoolClass classe) {
        if (classe.getHeadTeacherId() == null) {
            return "—";
        }
        return teacherRepository.findById(classe.getHeadTeacherId())
                .map(enseignant -> enseignant.getFirstName() + " " + enseignant.getLastName())
                .orElse("—");
    }

    private String anneeScolaire(ReportCard bulletin) {
        return schoolYearService.active().map(SchoolYear::getLabel).orElseGet(() -> {
            int premiere = bulletin.getPeriodFrom().getMonthValue() >= 8
                    ? bulletin.getPeriodFrom().getYear()
                    : bulletin.getPeriodFrom().getYear() - 1;
            return premiere + "-" + (premiere + 1);
        });
    }

    /**
     * Le sexe est du texte libre en base (un effectif importé peut porter autre chose que F
     * ou M) : on compte ce qui est reconnaissable et on ne force pas le reste dans une case.
     * Filles + garçons peut donc être inférieur au total, et c'est plus honnête que de
     * répartir au hasard.
     */
    private TermReport.Effectif effectif(List<Student> eleves, List<ReportCard> bulletins) {
        long filles = eleves.stream().filter(e -> "F".equalsIgnoreCase(valeur(e.getGender()))).count();
        long garcons = eleves.stream().filter(e -> "M".equalsIgnoreCase(valeur(e.getGender()))).count();
        long evalues = bulletins.stream().filter(b -> b.getGeneralAverage() != null).count();
        return new TermReport.Effectif(eleves.size(), (int) filles, (int) garcons, (int) evalues);
    }

    private TermReport.Resultats resultats(List<ReportCard> bulletins) {
        List<Double> moyennes = bulletins.stream()
                .map(ReportCard::getGeneralAverage)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
        int sansNote = bulletins.size() - moyennes.size();
        if (moyennes.isEmpty()) {
            return new TermReport.Resultats(null, null, null, 0, null, sansNote);
        }
        long admis = moyennes.stream().filter(m -> m >= 10).count();
        return new TermReport.Resultats(
                NumberUtils.round2(moyennes.stream().mapToDouble(Double::doubleValue).average().orElse(0)),
                moyennes.get(moyennes.size() - 1),
                moyennes.get(0),
                (int) admis,
                // Rapporté aux élèves évalués, pas à l'effectif : un élève sans note n'a pas
                // échoué, et l'inclure au dénominateur ferait baisser un taux sans raison.
                NumberUtils.round2(admis * 100.0 / moyennes.size()),
                sansNote);
    }

    private List<TermReport.LigneMatiere> matieres(List<ReportCard> bulletins) {
        Map<Long, List<ReportCardEntry>> parMatiere = bulletins.stream()
                .flatMap(bulletin -> reportCardService.listEntries(bulletin.getId()).stream())
                .filter(ligne -> ligne.getAverage() != null)
                .collect(Collectors.groupingBy(ReportCardEntry::getSubjectId));
        if (parMatiere.isEmpty()) {
            return List.of();
        }
        Map<Long, Subject> matieresParId = subjectRepository.findAllById(parMatiere.keySet()).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));

        List<TermReport.LigneMatiere> lignes = new ArrayList<>();
        parMatiere.forEach((matiereId, entrees) -> {
            List<Double> notes = entrees.stream().map(ReportCardEntry::getAverage).sorted().toList();
            long atteignent = notes.stream().filter(n -> n >= 10).count();
            Subject matiere = matieresParId.get(matiereId);
            lignes.add(new TermReport.LigneMatiere(
                    matiere == null ? "Matière #" + matiereId : matiere.getName(),
                    entrees.get(0).getCoefficient(),
                    NumberUtils.round2(notes.stream().mapToDouble(Double::doubleValue).average().orElse(0)),
                    notes.get(notes.size() - 1),
                    notes.get(0),
                    notes.size(),
                    (int) atteignent,
                    NumberUtils.round2(atteignent * 100.0 / notes.size())));
        });
        // Les matières les plus faibles en haut : c'est ce sur quoi la tutelle demande un plan.
        lignes.sort(Comparator.comparingDouble(TermReport.LigneMatiere::moyenne));
        return lignes;
    }

    private List<TermReport.Tranche> repartition(List<ReportCard> bulletins) {
        List<Double> moyennes = bulletins.stream()
                .map(ReportCard::getGeneralAverage)
                .filter(java.util.Objects::nonNull)
                .toList();
        List<TermReport.Tranche> tranches = new ArrayList<>();
        for (int i = 0; i < BORNES.length - 1; i++) {
            double bas = BORNES[i];
            double haut = BORNES[i + 1];
            long effectif = moyennes.stream().filter(m -> m >= bas && m < haut).count();
            String libelle = i == BORNES.length - 2
                    ? String.format("%.0f à 20", bas)
                    : String.format("%.0f à %.0f", bas, haut);
            tranches.add(new TermReport.Tranche(
                    libelle,
                    (int) effectif,
                    moyennes.isEmpty() ? null : NumberUtils.round2(effectif * 100.0 / moyennes.size())));
        }
        return tranches;
    }

    private TermReport.Assiduite assiduite(Long schoolClassId, ReportCard periode, int effectif) {
        List<AttendanceRecord> releves = attendanceRepository.findAllBySchoolClassIdAndDateBetween(
                schoolClassId, periode.getPeriodFrom(), periode.getPeriodTo());
        long absences = releves.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        long retards = releves.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        long justifiees = releves.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.ABSENT && r.isJustified())
                .count();
        // Rapporté aux appels réellement faits, pas aux jours du calendrier : une classe dont
        // on n'a fait l'appel que trois fois ne doit pas afficher 2 % de présence.
        Double taux = releves.isEmpty()
                ? null
                : NumberUtils.round2((releves.size() - absences) * 100.0 / releves.size());
        return new TermReport.Assiduite((int) absences, (int) retards, (int) justifiees, taux);
    }

    private TermReport.Discipline discipline(Long schoolClassId, ReportCard periode, List<Student> eleves) {
        List<Incident> incidents = incidentRepository.findAllBySchoolClassIdAndOccurredAtBetween(
                schoolClassId, periode.getPeriodFrom(), periode.getPeriodTo());
        int sanctions = incidents.stream()
                .mapToInt(incident -> sanctionRepository.findAllByIncidentId(incident.getId()).size())
                .sum();
        List<Long> ids = eleves.stream().map(Student::getId).toList();
        long positives = ids.isEmpty() ? 0 : observationRepository.findAllByStudentIdIn(ids).stream()
                .filter(Observation::isPositive)
                .count();
        return new TermReport.Discipline(incidents.size(), sanctions, (int) positives);
    }

    private static String valeur(String texte) {
        return texte == null ? "" : texte.trim();
    }
}
