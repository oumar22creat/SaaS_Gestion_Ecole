package com.schoolsaas.discipline;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.discipline.dto.ConvocationCreateRequest;
import com.schoolsaas.discipline.dto.ConvocationResponse;
import com.schoolsaas.discipline.dto.DisciplineStatisticsResponse;
import com.schoolsaas.discipline.dto.IncidentCreateRequest;
import com.schoolsaas.discipline.dto.ObservationCreateRequest;
import com.schoolsaas.discipline.dto.ObservationResponse;
import com.schoolsaas.discipline.dto.SanctionCreateRequest;
import com.schoolsaas.discipline.dto.SanctionResponse;
import com.schoolsaas.discipline.dto.StudentDisciplineHistoryResponse;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.StudentRepository;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vie scolaire — cahier-des-charges.md §17, ROADMAP.md 3.1. */
@Service
public class DisciplineService {

    private final IncidentRepository incidentRepository;
    private final IncidentStudentRepository incidentStudentRepository;
    private final SanctionRepository sanctionRepository;
    private final ConvocationRepository convocationRepository;
    private final ObservationRepository observationRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    public DisciplineService(
            IncidentRepository incidentRepository,
            IncidentStudentRepository incidentStudentRepository,
            SanctionRepository sanctionRepository,
            ConvocationRepository convocationRepository,
            ObservationRepository observationRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository) {
        this.incidentRepository = incidentRepository;
        this.incidentStudentRepository = incidentStudentRepository;
        this.sanctionRepository = sanctionRepository;
        this.convocationRepository = convocationRepository;
        this.observationRepository = observationRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public Incident createIncident(IncidentCreateRequest request, Long reporterUserId) {
        if (schoolClassRepository.findById(request.schoolClassId()).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
        for (Long studentId : request.studentIds()) {
            if (studentRepository.findById(studentId).isEmpty()) {
                throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable : " + studentId);
            }
        }
        Incident incident = incidentRepository.save(new Incident(
                request.schoolClassId(), request.occurredAt(), request.severity(), request.description(), reporterUserId));
        for (Long studentId : request.studentIds()) {
            incidentStudentRepository.save(new IncidentStudent(incident.getId(), studentId));
        }
        return incident;
    }

    public Incident getIncident(Long id) {
        return incidentRepository.findById(id).orElseThrow(() -> ApiException.notFound("INCIDENT_NOT_FOUND", "Incident introuvable"));
    }

    public List<Long> studentIdsOfIncident(Long incidentId) {
        return incidentStudentRepository.findAllByIncidentId(incidentId).stream().map(IncidentStudent::getStudentId).toList();
    }

    public List<Incident> listIncidentsForClass(Long schoolClassId, LocalDate from, LocalDate to) {
        return incidentRepository.findAllBySchoolClassIdAndOccurredAtBetween(schoolClassId, from, to);
    }

    @Transactional
    public Sanction createSanction(Long incidentId, SanctionCreateRequest request, Long deciderUserId) {
        getIncident(incidentId);
        if (!studentIdsOfIncident(incidentId).contains(request.studentId())) {
            throw ApiException.badRequest(
                    "STUDENT_NOT_IN_INCIDENT", "L'élève n'est pas déclaré comme concerné par cet incident", List.of());
        }
        return sanctionRepository.save(new Sanction(
                incidentId, request.studentId(), request.type(), request.durationDays(), request.description(), deciderUserId));
    }

    public List<Sanction> listSanctionsForIncident(Long incidentId) {
        getIncident(incidentId);
        return sanctionRepository.findAllByIncidentId(incidentId);
    }

    @Transactional
    public Convocation createConvocation(ConvocationCreateRequest request, Long creatorUserId) {
        requireStudent(request.studentId());
        return convocationRepository.save(
                new Convocation(request.studentId(), request.convokeParent(), request.reason(), request.scheduledAt(), creatorUserId));
    }

    @Transactional
    public Convocation updateConvocationStatus(Long id, ConvocationStatus status) {
        Convocation convocation = convocationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CONVOCATION_NOT_FOUND", "Convocation introuvable"));
        convocation.setStatus(status);
        return convocation;
    }

    @Transactional
    public Observation createObservation(ObservationCreateRequest request, Long authorUserId) {
        requireStudent(request.studentId());
        return observationRepository.save(
                new Observation(request.studentId(), request.positive(), request.description(), authorUserId));
    }

    public StudentDisciplineHistoryResponse historyForStudent(Long studentId) {
        requireStudent(studentId);
        List<SanctionResponse> sanctions = sanctionRepository.findAllByStudentIdOrderByDecidedAtDesc(studentId).stream()
                .map(SanctionResponse::from)
                .toList();
        List<ObservationResponse> observations = observationRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(ObservationResponse::from)
                .toList();
        List<ConvocationResponse> convocations = convocationRepository.findAllByStudentIdOrderByScheduledAtDesc(studentId).stream()
                .map(ConvocationResponse::from)
                .toList();
        return new StudentDisciplineHistoryResponse(sanctions, observations, convocations);
    }

    public DisciplineStatisticsResponse statisticsForClass(Long schoolClassId, LocalDate from, LocalDate to) {
        List<Incident> incidents = listIncidentsForClass(schoolClassId, from, to);
        Map<IncidentSeverity, Long> incidentsBySeverity = new EnumMap<>(IncidentSeverity.class);
        for (Incident incident : incidents) {
            incidentsBySeverity.merge(incident.getSeverity(), 1L, Long::sum);
        }
        Map<SanctionType, Long> sanctionsByType = incidents.stream()
                .flatMap(incident -> sanctionRepository.findAllByIncidentId(incident.getId()).stream())
                .collect(Collectors.groupingBy(Sanction::getType, () -> new EnumMap<>(SanctionType.class), Collectors.counting()));
        return new DisciplineStatisticsResponse(schoolClassId, from, to, incidents.size(), incidentsBySeverity, sanctionsByType);
    }

    private void requireStudent(Long studentId) {
        if (studentRepository.findById(studentId).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable");
        }
    }
}
