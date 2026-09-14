package com.schoolsaas.transport;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.transport.dto.BusRouteCreateRequest;
import com.schoolsaas.transport.dto.BusStopCreateRequest;
import com.schoolsaas.transport.dto.TransportAssignmentRequest;
import com.schoolsaas.transport.dto.TransportInvoiceGenerateRequest;
import com.schoolsaas.transport.dto.TransportPaymentCreateRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transport scolaire — cahier-des-charges.md §19.2, ROADMAP.md 3.6. */
@Service
public class TransportService {

    private final BusRouteRepository busRouteRepository;
    private final BusStopRepository busStopRepository;
    private final StudentTransportAssignmentRepository assignmentRepository;
    private final TransportInvoiceRepository invoiceRepository;
    private final TransportPaymentRepository paymentRepository;
    private final StudentRepository studentRepository;

    public TransportService(
            BusRouteRepository busRouteRepository,
            BusStopRepository busStopRepository,
            StudentTransportAssignmentRepository assignmentRepository,
            TransportInvoiceRepository invoiceRepository,
            TransportPaymentRepository paymentRepository,
            StudentRepository studentRepository) {
        this.busRouteRepository = busRouteRepository;
        this.busStopRepository = busStopRepository;
        this.assignmentRepository = assignmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
    }

    public BusRoute createRoute(BusRouteCreateRequest request) {
        return busRouteRepository.save(new BusRoute(request.label()));
    }

    public List<BusRoute> listRoutes() {
        return busRouteRepository.findAll();
    }

    public BusRoute getRoute(Long id) {
        return busRouteRepository.findById(id).orElseThrow(() -> ApiException.notFound("ROUTE_NOT_FOUND", "Ligne introuvable"));
    }

    public BusStop addStop(Long routeId, BusStopCreateRequest request) {
        getRoute(routeId);
        return busStopRepository.save(new BusStop(routeId, request.name(), request.sequenceOrder()));
    }

    public List<BusStop> listStops(Long routeId) {
        return busStopRepository.findAllByBusRouteIdOrderBySequenceOrderAsc(routeId);
    }

    @Transactional
    public StudentTransportAssignment assignStudent(TransportAssignmentRequest request) {
        if (studentRepository.findById(request.studentId()).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable");
        }
        getRoute(request.busRouteId());
        BusStop stop = busStopRepository.findById(request.busStopId())
                .orElseThrow(() -> ApiException.notFound("STOP_NOT_FOUND", "Arrêt introuvable"));
        if (!stop.getBusRouteId().equals(request.busRouteId())) {
            throw ApiException.badRequest("STOP_NOT_ON_ROUTE", "Cet arrêt n'appartient pas à cette ligne", List.of());
        }

        return assignmentRepository.findByStudentId(request.studentId())
                .map(existing -> {
                    existing.setBusRouteId(request.busRouteId());
                    existing.setBusStopId(request.busStopId());
                    return existing;
                })
                .orElseGet(() -> assignmentRepository.save(
                        new StudentTransportAssignment(request.studentId(), request.busRouteId(), request.busStopId())));
    }

    public StudentTransportAssignment getAssignmentForStudent(Long studentId) {
        return assignmentRepository.findByStudentId(studentId)
                .orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND", "Élève non affecté à un circuit"));
    }

    @Transactional
    public TransportInvoice generateInvoice(TransportInvoiceGenerateRequest request) {
        getAssignmentForStudent(request.studentId());
        return invoiceRepository.save(
                new TransportInvoice(request.studentId(), request.periodFrom(), request.periodTo(), request.amountDueCents()));
    }

    public TransportInvoice getInvoice(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> ApiException.notFound("INVOICE_NOT_FOUND", "Facture introuvable"));
    }

    public List<TransportInvoice> invoicesForStudent(Long studentId) {
        return invoiceRepository.findAllByStudentIdOrderByIssuedAtDesc(studentId);
    }

    public long paidAmountForInvoice(Long invoiceId) {
        return paymentRepository.findAllByInvoiceId(invoiceId).stream().mapToLong(TransportPayment::getAmountCents).sum();
    }

    public List<TransportPayment> paymentsForInvoice(Long invoiceId) {
        return paymentRepository.findAllByInvoiceId(invoiceId);
    }

    @Transactional
    public TransportPayment recordPayment(Long invoiceId, TransportPaymentCreateRequest request, Long recordedByUserId) {
        TransportInvoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() == TransportInvoiceStatus.CANCELLED) {
            throw ApiException.badRequest("INVOICE_CANCELLED", "Cette facture est annulée", List.of());
        }
        TransportPayment payment = paymentRepository.save(
                new TransportPayment(invoiceId, request.amountCents(), request.method(), request.reference(), recordedByUserId));

        long totalPaid = paidAmountForInvoice(invoiceId);
        invoice.setStatus(
                totalPaid >= invoice.getAmountDueCents() ? TransportInvoiceStatus.PAID
                        : totalPaid > 0 ? TransportInvoiceStatus.PARTIALLY_PAID : TransportInvoiceStatus.PENDING);
        return payment;
    }
}
