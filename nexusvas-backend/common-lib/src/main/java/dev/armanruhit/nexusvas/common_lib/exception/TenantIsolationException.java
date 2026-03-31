package dev.armanruhit.nexusvas.common_lib.exception;

public class TenantIsolationException extends NexusVasException {
    public TenantIsolationException(String tenantId) {
        super("Access denied for tenant: " + tenantId, "TENANT_ISOLATION_VIOLATION");
    }
}
