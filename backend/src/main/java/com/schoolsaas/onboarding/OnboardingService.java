package com.schoolsaas.onboarding;

import com.schoolsaas.onboarding.dto.OnboardingStatusResponse;
import com.schoolsaas.onboarding.dto.OnboardingStatusResponse.Step;
import com.schoolsaas.schoolclass.SchoolClassRepository;
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

    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TimetableEntryRepository timetableEntryRepository;

    public OnboardingService(
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            TimetableEntryRepository timetableEntryRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.timetableEntryRepository = timetableEntryRepository;
    }

    public OnboardingStatusResponse status() {
        List<Step> steps = List.of(
                Step.of("CLASSES", schoolClassRepository.count()),
                Step.of("SUBJECTS", subjectRepository.count()),
                Step.of("STUDENTS", studentRepository.countByActiveTrue()),
                Step.of("TEACHERS", teacherRepository.countByActiveTrue()),
                Step.of("TIMETABLE", timetableEntryRepository.count()));
        return new OnboardingStatusResponse(steps, steps.stream().allMatch(Step::done));
    }
}
