package com.aurix.platform.openfinance.tenant;

/**
 * Contexto de execução multi-tenant baseado em ThreadLocal.
 *
 * <p>Permite que uma única instância do Aurix Open Finance Gateway atenda
 * múltiplas cooperativas, bancos ou marcas de forma logicamente isolada.
 */
public final class TenantContext {

    private static final String DEFAULT_TENANT_ID = "aurix";
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        return tenant != null && !tenant.isBlank() ? tenant : DEFAULT_TENANT_ID;
    }

    public static void setTenantId(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            CURRENT_TENANT.set(tenantId.trim().toLowerCase());
        } else {
            CURRENT_TENANT.remove();
        }
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
