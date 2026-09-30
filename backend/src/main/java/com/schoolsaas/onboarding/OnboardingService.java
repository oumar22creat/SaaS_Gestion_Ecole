package com.schoolsaas.onboarding;

import com.schoolsaas.onboarding.dto.OnboardingStatusResponse;
import com.schoolsaas.onboarding.dto.OnboardingStatusResponse.Step;
import com.schoolsaas.timetable.RoomRepository;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolyear.SchoolYearService;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.timetable.TimetableEntryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Configuration initiale d'un établissement (ROADMAP.md 1.3). Une école qui vient de
 * s'inscrire arrive sur une application vide : rien ne lui dit qu'il faut créer les classes
 * avant de pouvoir bâtir un emploi du temps, ni que les élèves s'importent en masse.
 *
 * <p>L'avancement est <b>déduit des données réelles</b>, jamais stocké dans un drapeau : une
 * école qui supprime toutes ses classes revoit l'étape correspondante, et aucun état ne peut
 * diverger de la réalité. C'est aussi ce qui rend l'assistant utilisable par un établissement
 * déjà configuré avant l'existence de cette page.
 */
@Service
public class OnboardingService {

    /** Ordre de la configuration (cahier §20.2) : chaque étape s'appuie sur les précédentes. */
    static final List<String> STEP_ORDER =
            List.of("CLASSES", "SUBJECTS", "STUDENTS", "TEACHERS", "TIMETABLE");

    private final SchoolYearService schoolYearService;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final TimetableEntryRepository timetableEntryRepository;

    public OnboardingService(
            SchoolYearService schoolYearService,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            RoomRepository roomRepository,
            TimetableEntryRepository timetableEntryRepository) {
        this.schoolYearService = schoolYearService;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
        this.timetableEntryRepository = timetableEntryRepository;
    }

    public OnboardingStatusResponse status() {
        List<Step> steps = List.of(
                // En tête, parce que c'est un prérequis et non une étape parmi d'autres : sans
                // année active, ni certificat de scolarité, ni inscription, ni bulletin. Un
                // établissement pouvait franchir tout le guide et buter dessus au guichet.
                Step.of("SCHOOL_YEAR", schoolYearService.active().isPresent() ? 1 : 0),
                Step.of("CLASSES", schoolClassRepository.count()),
                Step.of("SUBJECTS", subjectRepository.count()),
                Step.of("STUDENTS", studentRepository.countByActiveTrue()),
                Step.of("TEACHERS", teacherRepository.countByActiveTrue()),
                // Facultative, et placée juste avant l'emploi du temps, seul écran qui s'en
                // sert : la liste des salles y était vide sans que rien n'ait jamais invité à
                // la remplir.
                Step.optionnelle("ROOMS", roomRepository.count()),
                Step.of("TIMETABLE", timetableEntryRepository.count()));
        // Une étape facultative non faite ne retient pas l'achèvement du guide.
        return new OnboardingStatusResponse(
                steps, steps.stream().filter(Step::required).allMatch(Step::done));
    }
}
