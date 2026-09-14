package com.schoolsaas.timetable;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.timetable.dto.TimetableEntryRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD emploi du temps + détection de conflits — cahier-des-charges.md §9, ROADMAP.md 1.6. */
@Service
public class TimetableEntryService {

    private final TimetableEntryRepository timetableEntryRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;

    public TimetableEntryService(
            TimetableEntryRepository timetableEntryRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            RoomRepository roomRepository) {
        this.timetableEntryRepository = timetableEntryRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public TimetableEntry create(TimetableEntryRequest request) {
        validateReferences(request);
        detectConflicts(request, null);
        return timetableEntryRepository.save(new TimetableEntry(
                request.schoolClassId(), request.subjectId(), request.teacherId(), request.roomId(),
                request.dayOfWeek(), request.startTime(), request.endTime()));
    }

    public TimetableEntry getById(Long id) {
        return timetableEntryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("TIMETABLE_ENTRY_NOT_FOUND", "Créneau introuvable"));
    }

    public List<TimetableEntry> listForClass(Long schoolClassId) {
        return timetableEntryRepository.findAllBySchoolClassId(schoolClassId);
    }

    public List<TimetableEntry> listForTeacher(Long teacherId) {
        return timetableEntryRepository.findAllByTeacherId(teacherId);
    }

    public List<TimetableEntry> listAll() {
        return timetableEntryRepository.findAll();
    }

    @Transactional
    public TimetableEntry update(Long id, TimetableEntryRequest request) {
        validateReferences(request);
        detectConflicts(request, id);
        TimetableEntry entry = getById(id);
        entry.setSchoolClassId(request.schoolClassId());
        entry.setSubjectId(request.subjectId());
        entry.setTeacherId(request.teacherId());
        entry.setRoomId(request.roomId());
        entry.setDayOfWeek(request.dayOfWeek());
        entry.setStartTime(request.startTime());
        entry.setEndTime(request.endTime());
        return entry;
    }

    @Transactional
    public void delete(Long id) {
        timetableEntryRepository.delete(getById(id));
    }

    /**
     * Détection des conflits salle/enseignant/classe (cahier-des-charges.md §9) : un même
     * enseignant, une même salle ou une même classe ne peuvent avoir deux créneaux qui se
     * chevauchent le même jour. {@code excludingId} exclut le créneau lui-même lors d'une
     * mise à jour.
     */
    private void detectConflicts(TimetableEntryRequest request, Long excludingId) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw ApiException.badRequest("INVALID_TIME_RANGE", "L'heure de fin doit être après l'heure de début", List.of());
        }

        List<TimetableEntry> sameDayForTeacher =
                timetableEntryRepository.findAllByDayOfWeekAndTeacherId(request.dayOfWeek(), request.teacherId());
        if (hasOverlap(sameDayForTeacher, request, excludingId)) {
            throw ApiException.conflict("TEACHER_ALREADY_BOOKED", "Cet enseignant a déjà un cours sur ce créneau");
        }

        List<TimetableEntry> sameDayForRoom =
                timetableEntryRepository.findAllByDayOfWeekAndRoomId(request.dayOfWeek(), request.roomId());
        if (hasOverlap(sameDayForRoom, request, excludingId)) {
            throw ApiException.conflict("ROOM_ALREADY_BOOKED", "Cette salle est déjà occupée sur ce créneau");
        }

        List<TimetableEntry> sameDayForClass =
                timetableEntryRepository.findAllByDayOfWeekAndSchoolClassId(request.dayOfWeek(), request.schoolClassId());
        if (hasOverlap(sameDayForClass, request, excludingId)) {
            throw ApiException.conflict("CLASS_ALREADY_BOOKED", "Cette classe a déjà un cours sur ce créneau");
        }
    }

    private boolean hasOverlap(List<TimetableEntry> existingEntries, TimetableEntryRequest request, Long excludingId) {
        return existingEntries.stream()
                .filter(entry -> !Objects.equals(entry.getId(), excludingId))
                .anyMatch(entry -> entry.overlaps(request.startTime(), request.endTime()));
    }

    private void validateReferences(TimetableEntryRequest request) {
        if (schoolClassRepository.findById(request.schoolClassId()).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
        if (subjectRepository.findById(request.subjectId()).isEmpty()) {
            throw ApiException.notFound("SUBJECT_NOT_FOUND", "Matière introuvable");
        }
        if (teacherRepository.findById(request.teacherId()).isEmpty()) {
            throw ApiException.notFound("TEACHER_NOT_FOUND", "Enseignant introuvable");
        }
        if (roomRepository.findById(request.roomId()).isEmpty()) {
            throw ApiException.notFound("ROOM_NOT_FOUND", "Salle introuvable");
        }
    }
}
