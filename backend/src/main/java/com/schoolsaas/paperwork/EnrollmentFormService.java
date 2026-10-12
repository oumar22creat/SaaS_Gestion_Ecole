package com.schoolsaas.paperwork;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParent;
import com.schoolsaas.parent.StudentParentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolfees.FeeSchedule;
import com.schoolsaas.schoolfees.FeeScheduleRepository;
import com.schoolsaas.schoolyear.SchoolYear;
import com.schoolsaas.schoolyear.SchoolYearService;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentPhotoService;
import com.schoolsaas.student.StudentRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Rassemble ce qui compose la fiche d'inscription d'un élève. */
@Service
public class EnrollmentFormService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentParentRepository studentParentRepository;
    private final ParentRepository parentRepository;
    private final FeeScheduleRepository feeScheduleRepository;
    private final StudentPhotoService photoService;
    private final SchoolYearService schoolYearService;

    public EnrollmentFormService(
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            StudentParentRepository studentParentRepository,
            ParentRepository parentRepository,
            FeeScheduleRepository feeScheduleRepository,
            StudentPhotoService photoService,
            SchoolYearService schoolYearService) {
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentParentRepository = studentParentRepository;
        this.parentRepository = parentRepository;
        this.feeScheduleRepository = feeScheduleRepository;
        this.photoService = photoService;
        this.schoolYearService = schoolYearService;
    }

    public EnrollmentFormPdfExporter.Donnees build(Long studentId) {
        Student eleve = studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));

        String classe = eleve.getSchoolClassId() == null
                ? null
                : schoolClassRepository.findById(eleve.getSchoolClassId())
                        .map(SchoolClass::getName)
                        .orElse(null);

        return new EnrollmentFormPdfExporter.Donnees(
                eleve.getLastName(),
                eleve.getFirstName(),
                eleve.getStudentNumber(),
                eleve.getBirthDate(),
                eleve.getGender(),
                classe,
                anneeScolaire(),
                photoService.contenu(eleve).orElse(null),
                responsables(studentId),
                frais(eleve.getSchoolClassId()));
    }

    /**
     * Le contact principal en tête : c'est celui que le secrétariat appelle, et il doit se
     * lire sans parcourir la liste.
     */
    private List<EnrollmentFormPdfExporter.Responsable> responsables(Long studentId) {
        List<StudentParent> liens = studentParentRepository.findAllByStudentId(studentId);
        if (liens.isEmpty()) {
            return List.of();
        }
        Map<Long, Parent> parents = parentRepository
                .findAllById(liens.stream().map(StudentParent::getParentId).toList()).stream()
                .collect(Collectors.toMap(Parent::getId, Function.identity()));

        return liens.stream()
                .sorted(Comparator.comparing(StudentParent::isPrimaryContact).reversed())
                .map(lien -> {
                    Parent parent = parents.get(lien.getParentId());
                    if (parent == null) {
                        return null;
                    }
                    return new EnrollmentFormPdfExporter.Responsable(
                            parent.getLastName().toUpperCase(java.util.Locale.FRENCH) + " " + parent.getFirstName(),
                            lien.getRelationship(),
                            parent.getPhone(),
                            parent.getEmail(),
                            lien.isPrimaryContact());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** L'échéancier de la classe, dans l'ordre où les sommes tombent. */
    private List<EnrollmentFormPdfExporter.Frais> frais(Long schoolClassId) {
        if (schoolClassId == null) {
            return List.of();
        }
        return feeScheduleRepository.findAllBySchoolClassId(schoolClassId).stream()
                .sorted(Comparator.comparing(
                        FeeSchedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(echeance -> new EnrollmentFormPdfExporter.Frais(
                        echeance.getLabel(),
                        echeance.getDueDate(),
                        echeance.getAmountCents(),
                        echeance.getCurrency()))
                .toList();
    }

    /** Libellé de l'année active, ou les deux années civiles à cheval si aucune n'est ouverte. */
    private String anneeScolaire() {
        return schoolYearService.active().map(SchoolYear::getLabel).orElseGet(() -> {
            int premiere = LocalDate.now().getMonthValue() >= 8
                    ? LocalDate.now().getYear()
                    : LocalDate.now().getYear() - 1;
            return premiere + "-" + (premiere + 1);
        });
    }
}
