package com.schoolsaas.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import com.schoolsaas.common.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test unitaire du blocage progressif (voir docs/ARCHITECTURE.md ADR-009) : pas besoin d'un
 * vrai endpoint métier mutant pour vérifier la logique (aucun n'existe encore avant la
 * Phase 1.5) — {@link MockHttpServletRequest}/{@link MockHttpServletResponse} (déjà sur le
 * classpath via spring-test) suffisent à simuler n'importe quelle route/méthode HTTP.
 */
@ExtendWith(MockitoExtension.class)
class TenantAccessInterceptorTest {

    @Mock
    private TenantRepository tenantRepository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void doesNothingWhenNoTenantContext() {
        TenantContext.clear();
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        boolean result = interceptor.preHandle(request("POST", "/api/v1/students"), new MockHttpServletResponse(), null);

        assertThat(result).isTrue();
        verifyNoInteractions(tenantRepository);
    }

    @Test
    void allowsAnyMethodWhenTenantIsActive() {
        TenantContext.set(1L);
        stubTenant(1L, TenantStatus.ACTIVE);
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        assertThat(interceptor.preHandle(request("POST", "/api/v1/students"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void readOnlyTenantCanStillRead() {
        TenantContext.set(2L);
        stubTenant(2L, TenantStatus.READ_ONLY);
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        assertThat(interceptor.preHandle(request("GET", "/api/v1/students"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void readOnlyTenantCannotWriteOutsideAllowlist() {
        TenantContext.set(3L);
        stubTenant(3L, TenantStatus.READ_ONLY);
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        assertThatThrownBy(() -> interceptor.preHandle(request("POST", "/api/v1/students"), new MockHttpServletResponse(), null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("TENANT_READ_ONLY"));
    }

    @Test
    void readOnlyTenantCanStillWriteToBillingToReactivate() {
        TenantContext.set(4L);
        stubTenant(4L, TenantStatus.READ_ONLY);
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        assertThat(interceptor.preHandle(request("POST", "/api/v1/billing/checkout"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void suspendedTenantIsBlockedEvenOnReads() {
        TenantContext.set(5L);
        stubTenant(5L, TenantStatus.SUSPENDED);
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        assertThatThrownBy(() -> interceptor.preHandle(request("GET", "/api/v1/users/me"), new MockHttpServletResponse(), null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("TENANT_SUSPENDED"));
    }

    @Test
    void suspendedTenantCanStillLogInAndReachBilling() {
        TenantContext.set(6L);
        stubTenant(6L, TenantStatus.SUSPENDED);
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        assertThat(interceptor.preHandle(request("POST", "/api/v1/auth/login"), new MockHttpServletResponse(), null)).isTrue();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/billing/subscription"), new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void cancelledTenantIsBlockedLikeSuspended() {
        TenantContext.set(7L);
        stubTenant(7L, TenantStatus.CANCELLED);
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor(tenantRepository);

        assertThatThrownBy(() -> interceptor.preHandle(request("GET", "/api/v1/users/me"), new MockHttpServletResponse(), null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("TENANT_SUSPENDED"));
    }

    private void stubTenant(Long tenantId, TenantStatus status) {
        lenient().when(tenantRepository.findById(tenantId))
                .thenReturn(Optional.of(new Tenant("École", "ecole-" + tenantId, status)));
    }

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }
}
