package com.schoolsaas.homework;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.document.DocumentRepository;
import com.schoolsaas.homework.dto.LessonRequest;
import com.schoolsaas.notification.NotificationDispatcher;
import com.schoolsaas.notification.NotificationType;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.subject.SubjectRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cahier de textes et devoirs — cahier-des-charges.md §13, ROADMAP.md 2.3. */
@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final DocumentRepository documentRepository;
    private final NotificationDispatcher notificationDispatcher;

    public LessonService(
            LessonRepository lessonRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            DocumentRepository documentRepository,
            NotificationDispatcher notificationDispatcher) {
        this.lessonRepository = lessonRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.documentRepository = documentRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public Lesson create(LessonRequest request) {
        validateReferences(request);
        Lesson lesson = lessonRepository.save(
                new Lesson(request.schoolClassId(), request.subjectId(), request.sessionDate(), request.content()));
        lesson.setHomework(request.homework());
        lesson.setHomeworkDueDate(request.homeworkDueDate());
        lesson.setAttachmentDocumentId(request.attachmentDocumentId());
        if (request.homework() != null) {
            notifyNewHomework(lesson);
        }
        return lesson;
    }

    public Lesson getById(Long id) {
        return lessonRepository.findById(id).orElseThrow(() -> ApiException.notFound("LESSON_NOT_FOUND", "Séance introuvable"));
    }

    public List<Lesson> listForClass(Long schoolClassId, LocalDate from, LocalDate to) {
        return lessonRepository.findAllBySchoolClassIdAndSessionDateBetween(schoolClassId, from, to);
    }

    public Page<Lesson> list(Pageable pageable) {
        return lessonRepository.findAll(pageable);
    }

    @Transactional
    public Lesson update(Long id, LessonRequest request) {
        validateReferences(request);
        Lesson lesson = getById(id);
        boolean isNewHomework = request.homework() != null && !Objects.equals(request.homework(), lesson.getHomework());

        lesson.setSessionDate(request.sessionDate());
        lesson.setContent(request.content());
        lesson.setHomework(request.homework());
        lesson.setHomeworkDueDate(request.homeworkDueDate());
        lesson.setAttachmentDocumentId(request.attachmentDocumentId());

        if (isNewHomework) {
            notifyNewHomework(lesson);
        }
        return lesson;
    }

    @Transactional
    public void delete(Long id) {
        lessonRepository.delete(getById(id));
    }

    /**
     * Notification nouveau devoir (cahier-des-charges.md §13/§16). Aucun compte utilisateur
     * destinataire réel (élèves/parents sans portail, voir ADR-010) :
     * {@link NotificationDispatcher} loggue simplement l'intention, voir ROADMAP.md 2.5.
     */
    private void notifyNewHomework(Lesson lesson) {
        notificationDispatcher.dispatch(
                NotificationType.NEW_HOMEWORK,
                List.of(),
                "Nouveau devoir",
                "Classe " + lesson.getSchoolClassId() + " — séance " + lesson.getId());
    }

    private void validateReferences(LessonRequest request) {
        if (schoolClassRepository.findById(request.schoolClassId()).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
        if (subjectRepository.findById(request.subjectId()).isEmpty()) {
            throw ApiException.notFound("SUBJECT_NOT_FOUND", "Matière introuvable");
        }
        if (request.attachmentDocumentId() != null && documentRepository.findById(request.attachmentDocumentId()).isEmpty()) {
            throw ApiException.notFound("DOCUMENT_NOT_FOUND", "Document introuvable");
        }
    }
}
