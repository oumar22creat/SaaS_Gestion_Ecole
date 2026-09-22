package com.schoolsaas.student;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolyear.SchoolYearService;
import com.schoolsaas.student.dto.StudentImportResult;
import com.schoolsaas.student.dto.StudentRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolYearService schoolYearService;

    public StudentService(
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolYearService schoolYearService) {
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolYearService = schoolYearService;
    }

    @Transactional
    public Student create(StudentRequest request) {
        validateSchoolClass(request.schoolClassId());
        if (studentRepository.existsByStudentNumber(request.studentNumber())) {
            throw ApiException.conflict("STUDENT_NUMBER_ALREADY_USED", "Ce matricule est déjà utilisé");
        }
        Student student = studentRepository.save(new Student(
                request.studentNumber(), request.firstName(), request.lastName(),
                request.birthDate(), request.gender(), request.schoolClassId()));
        recordEnrollment(student);
        return student;
    }

    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
    }

    public Page<Student> list(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @Transactional
    public Student update(Long id, StudentRequest request) {
        validateSchoolClass(request.schoolClassId());
        Student student = getById(id);
        if (!student.getStudentNumber().equals(request.studentNumber())
                && studentRepository.existsByStudentNumber(request.studentNumber())) {
            throw ApiException.conflict("STUDENT_NUMBER_ALREADY_USED", "Ce matricule est déjà utilisé");
        }
        student.setStudentNumber(request.studentNumber());
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setBirthDate(request.birthDate());
        student.setGender(request.gender());
        student.setSchoolClassId(request.schoolClassId());
        recordEnrollment(student);
        return student;
    }

    /**
     * Trace l'inscription sur l'année active, pour que la classe d'un élève ne soit plus une
     * donnée sans passé. Silencieux si aucune année n'est active : créer un élève ne doit pas
     * échouer parce que la direction n'a pas encore ouvert son année scolaire.
     */
    private void recordEnrollment(Student student) {
        if (student.getSchoolClassId() == null) {
            return;
        }
        schoolYearService.active().ifPresent(year ->
                schoolYearService.enrollInto(year, student.getId(), student.getSchoolClassId()));
    }

    @Transactional
    public void deactivate(Long id) {
        Student student = getById(id);
        student.setActive(false);
    }

    /**
     * Import en masse (cahier-des-charges.md §7) : CSV avec en-tête
     * {@code studentNumber,firstName,lastName,birthDate,gender,className} (colonnes
     * optionnelles vides autorisées sauf studentNumber/firstName/lastName). Parseur volontai-
     * rement simple (split virgule, pas de guillemets/champs contenant une virgule) — suffisant
     * pour un export tableur standard, à faire évoluer si un vrai besoin apparaît (voir
     * CLAUDE.md : pas de sur-ingénierie anticipée).
     */
    @Transactional
    public StudentImportResult importCsv(MultipartFile file) {
        List<StudentImportResult.RowError> errors = new ArrayList<>();
        int imported = 0;

        Map<String, Long> classIdsByName = schoolClassRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getName().trim().toLowerCase(), com.schoolsaas.schoolclass.SchoolClass::getId, (a, b) -> a));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw ApiException.badRequest("EMPTY_CSV_FILE", "Fichier CSV vide", List.of());
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    importRow(line, classIdsByName);
                    imported++;
                } catch (ApiException e) {
                    errors.add(new StudentImportResult.RowError(lineNumber, e.getMessage()));
                } catch (RuntimeException e) {
                    errors.add(new StudentImportResult.RowError(lineNumber, "Ligne invalide"));
                }
            }
        } catch (IOException e) {
            throw ApiException.badRequest("CSV_READ_ERROR", "Impossible de lire le fichier CSV", List.of());
        }

        return new StudentImportResult(imported, errors);
    }

    private void importRow(String line, Map<String, Long> classIdsByName) {
        String[] columns = line.split(",", -1);
        String studentNumber = column(columns, 0);
        String firstName = column(columns, 1);
        String lastName = column(columns, 2);
        String birthDateRaw = column(columns, 3);
        String gender = column(columns, 4);
        String className = column(columns, 5);

        if (studentNumber == null || firstName == null || lastName == null) {
            throw ApiException.badRequest("INVALID_ROW", "studentNumber, firstName et lastName sont obligatoires", List.of());
        }
        if (studentRepository.existsByStudentNumber(studentNumber)) {
            throw ApiException.conflict("STUDENT_NUMBER_ALREADY_USED", "Matricule déjà utilisé : " + studentNumber);
        }

        LocalDate birthDate = null;
        if (birthDateRaw != null) {
            try {
                birthDate = LocalDate.parse(birthDateRaw);
            } catch (java.time.format.DateTimeParseException e) {
                throw ApiException.badRequest("INVALID_BIRTH_DATE", "Date de naissance invalide (attendu AAAA-MM-JJ)", List.of());
            }
        }

        Long schoolClassId = className == null ? null : classIdsByName.get(className.trim().toLowerCase());

        studentRepository.save(new Student(studentNumber, firstName, lastName, birthDate, gender, schoolClassId));
    }

    private String column(String[] columns, int index) {
        if (index >= columns.length) {
            return null;
        }
        String value = columns[index].trim();
        return value.isEmpty() ? null : value;
    }

    private void validateSchoolClass(Long schoolClassId) {
        if (schoolClassId != null && schoolClassRepository.findById(schoolClassId).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
    }
}
