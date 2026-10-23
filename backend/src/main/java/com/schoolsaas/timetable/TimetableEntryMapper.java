package com.schoolsaas.timetable;

import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.timetable.dto.TimetableEntryResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transforme les créneaux en {@link TimetableEntryResponse} en y joignant les libellés
 * (classe, matière, enseignant, salle).
 *
 * <p>Quatre requêtes {@code findAllById} par lot, quel que soit le nombre de créneaux — pas de
 * requête par ligne. Les référentiels passent par le filtre tenant Hibernate et la RLS : un
 * identifiant d'un autre établissement ne donnerait aucun libellé, jamais celui du voisin.
 */
@Component
public class TimetableEntryMapper {

    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;

    public TimetableEntryMapper(
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            RoomRepository roomRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public TimetableEntryResponse toResponse(TimetableEntry entry) {
        return toResponses(List.of(entry)).get(0);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> toResponses(List<TimetableEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<Long, String> classNames = namesById(
                schoolClassRepository.findAllById(ids(entries, TimetableEntry::getSchoolClassId)),
                SchoolClass::getId, SchoolClass::getName);
        Map<Long, String> subjectNames = namesById(
                subjectRepository.findAllById(ids(entries, TimetableEntry::getSubjectId)),
                Subject::getId, Subject::getName);
        // Prénom et nom uniquement : l'e-mail et le téléphone de la fiche ne sortent pas d'ici.
        Map<Long, String> teacherNames = namesById(
                teacherRepository.findAllById(ids(entries, TimetableEntry::getTeacherId)),
                Teacher::getId, teacher -> (teacher.getFirstName() + " " + teacher.getLastName()).trim());
        Map<Long, String> roomNames = namesById(
                roomRepository.findAllById(ids(entries, TimetableEntry::getRoomId)),
                Room::getId, Room::getName);

        return entries.stream()
                .map(entry -> new TimetableEntryResponse(
                        entry.getId(),
                        entry.getSchoolClassId(),
                        classNames.get(entry.getSchoolClassId()),
                        entry.getSubjectId(),
                        subjectNames.get(entry.getSubjectId()),
                        entry.getTeacherId(),
                        teacherNames.get(entry.getTeacherId()),
                        entry.getRoomId(),
                        entry.getRoomId() == null ? null : roomNames.get(entry.getRoomId()),
                        entry.getDayOfWeek(),
                        entry.getStartTime(),
                        entry.getEndTime()))
                .toList();
    }

    /** Identifiants distincts et non nuls (la salle est facultative). */
    private static Collection<Long> ids(List<TimetableEntry> entries, Function<TimetableEntry, Long> idOf) {
        return entries.stream().map(idOf).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static <T> Map<Long, String> namesById(
            List<T> resources, Function<T, Long> idOf, Function<T, String> nameOf) {
        return resources.stream().collect(Collectors.toMap(idOf, nameOf, (first, second) -> first));
    }
}
