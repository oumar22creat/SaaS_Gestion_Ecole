package com.schoolsaas.mobilemoney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolfees.FeeInvoiceStatus;
import com.schoolsaas.schoolfees.FeeSchedule;
import com.schoolsaas.schoolfees.FeeScheduleRepository;
import com.schoolsaas.schoolfees.SchoolFeesService;
import com.schoolsaas.schoolfees.StudentFeeInvoice;
import com.schoolsaas.schoolfees.StudentFeeInvoiceRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantSessionConfigurer;
import com.schoolsaas.tenant.TenantStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Paiement mobile money. Les deux risques réels de ce module coûtent de l'argent à
 * quelqu'un : créditer deux fois une facture sur un rappel de l'opérateur, et laisser
 * n'importe qui déclarer un paiement via le callback public.
 */
@TestPropertySource(properties = "app.mobile-money.callback-secret=secret-de-test")
class MobileMoneyPaymentTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private StudentFeeInvoiceRepository invoiceRepository;
    @Autowired private FeeScheduleRepository feeScheduleRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private MobileMoneyPaymentRepository paymentRepository;
    @Autowired private MobileMoneyService mobileMoneyService;
    @Autowired private SchoolFeesService schoolFeesService;
    @Autowired private TenantSessionConfigurer tenantSessionConfigurer;
    @Autowired private ObjectMapper objectMapper;

    @Transactional
    Tenant tenantWithInvoice() {
        Tenant tenant = tenantRepository.save(
                new Tenant("École Test", "ecole-" + UUID.randomUUID(), TenantStatus.ACTIVE));
        TenantContext.set(tenant.getId());
        tenantSessionConfigurer.applyTenant(tenant.getId());
        return tenant;
    }

    private String callbackBody(String reference, boolean succeeded) throws Exception {
        return objectMapper.writeValueAsString(
                new MobileMoneyCallbackController.CallbackRequest(reference, succeeded, "OP-123", null));
    }

    @Test
    void aRepeatedCallbackCreditsTheInvoiceOnlyOnce() throws Exception {
        Tenant tenant = tenantWithInvoice();
        StudentFeeInvoice invoice = createInvoice(tenant, 50_000L);
        MobileMoneyPayment payment = mobileMoneyService.initiate(invoice.getId(), "22370000000");

        String body = callbackBody(payment.getReference(), true);
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/v1/payments/mobile-money/callback")
                            .header("X-Callback-Secret", "secret-de-test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());
        }

        TenantContext.set(tenant.getId());
        tenantSessionConfigurer.applyTenant(tenant.getId());
        assertThat(schoolFeesService.paidAmountForInvoice(invoice.getId())).isEqualTo(50_000L);
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus())
                .isEqualTo(FeeInvoiceStatus.PAID);
    }

    @Test
    void aCallbackWithoutTheRightSecretChangesNothing() throws Exception {
        Tenant tenant = tenantWithInvoice();
        StudentFeeInvoice invoice = createInvoice(tenant, 30_000L);
        MobileMoneyPayment payment = mobileMoneyService.initiate(invoice.getId(), "22370000001");

        mockMvc.perform(post("/api/v1/payments/mobile-money/callback")
                        .header("X-Callback-Secret", "mauvais-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(payment.getReference(), true)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payments/mobile-money/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(payment.getReference(), true)))
                .andExpect(status().isForbidden());

        TenantContext.set(tenant.getId());
        tenantSessionConfigurer.applyTenant(tenant.getId());
        assertThat(schoolFeesService.paidAmountForInvoice(invoice.getId())).isZero();
        assertThat(paymentRepository.findByReference(payment.getReference()).orElseThrow().getStatus())
                .isEqualTo(MobileMoneyStatus.PENDING);
    }

    @Test
    void aFailedPaymentLeavesTheInvoiceUntouched() throws Exception {
        Tenant tenant = tenantWithInvoice();
        StudentFeeInvoice invoice = createInvoice(tenant, 20_000L);
        MobileMoneyPayment payment = mobileMoneyService.initiate(invoice.getId(), "22370000002");

        mockMvc.perform(post("/api/v1/payments/mobile-money/callback")
                        .header("X-Callback-Secret", "secret-de-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(payment.getReference(), false)))
                .andExpect(status().isNoContent());

        TenantContext.set(tenant.getId());
        tenantSessionConfigurer.applyTenant(tenant.getId());
        assertThat(schoolFeesService.paidAmountForInvoice(invoice.getId())).isZero();
        assertThat(paymentRepository.findByReference(payment.getReference()).orElseThrow().getStatus())
                .isEqualTo(MobileMoneyStatus.FAILED);
    }

    /** Une référence forgée pour un autre établissement ne doit correspondre à aucune ligne. */
    @Test
    void aForgedReferenceIsRejected() throws Exception {
        tenantWithInvoice();

        mockMvc.perform(post("/api/v1/payments/mobile-money/callback")
                        .header("X-Callback-Secret", "secret-de-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody("OM-999999-inventee", true)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/payments/mobile-money/callback")
                        .header("X-Callback-Secret", "secret-de-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody("n-importe-quoi", true)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Transactional
    StudentFeeInvoice createInvoice(Tenant tenant, long amountCents) {
        SchoolClass schoolClass = new SchoolClass("6e A", null);
        schoolClass.setSchoolId(tenant.getId());
        schoolClass = schoolClassRepository.save(schoolClass);

        FeeSchedule schedule = new FeeSchedule(
                schoolClass.getId(), "Scolarité trimestre 1", amountCents, "XOF", java.time.LocalDate.now());
        schedule.setSchoolId(tenant.getId());
        schedule = feeScheduleRepository.save(schedule);

        Student student = new Student(
                "M-" + UUID.randomUUID().toString().substring(0, 8),
                "Fatoumata",
                "Sidibe",
                java.time.LocalDate.of(2012, 4, 3),
                "F",
                schoolClass.getId());
        student.setSchoolId(tenant.getId());
        student = studentRepository.save(student);

        StudentFeeInvoice invoice = new StudentFeeInvoice(schedule.getId(), student.getId(), amountCents);
        invoice.setSchoolId(tenant.getId());
        return invoiceRepository.save(invoice);
    }
}
