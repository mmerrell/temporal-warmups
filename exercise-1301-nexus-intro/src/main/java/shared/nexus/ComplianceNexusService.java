package shared.nexus;

import compliance.domain.ComplianceRequest;
import compliance.domain.ComplianceResult;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

/**
 * [STUDENT IMPLEMENTS] Nexus Service Interface — the shared contract between teams.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  THIS IS THE KEY NEW CONCEPT IN THIS EXERCISE
 * ═══════════════════════════════════════════════════════════════════
 *
 * Think of this as:
 *   - A durable, type-safe API contract between Payments and Compliance teams
 *   - Like a REST API definition, but with Temporal's durability guarantees
 *   - Both teams depend on this interface (shared library / shared package)
 *
 * METAPHOR: A REST API has:
 *   - An OpenAPI spec (the interface definition) ← you are here
 *   - A server controller (the @ServiceImpl)
 *   - A client SDK (the Nexus service stub in the workflow)
 *
 * Annotations:
 *   - @Service  (io.nexusrpc) marks this as a Nexus service interface
 *   - @Operation marks each method as a Nexus operation
 *
 * One operation in this exercise (sync only):
 *   checkCompliance() — validates a transaction against compliance rules, returns immediately
 *
 * ── Sync vs Async? ───────────────────────────────────────────────
 *
 * SYNC  = the handler runs inline and returns a result right away.
 *         Use when the work is fast (seconds). ← this exercise
 *
 * ASYNC = the handler starts a Temporal workflow on the Compliance side.
 *         Use when the work is long-running (minutes, hours).
 *         You'll implement async operations in Exercise 1300.
 *
 * ── How to implement this interface: ────────────────────────────
 *
 *   @Service
 *   public interface ComplianceNexusService {
 *
 *       @Operation
 *       ComplianceResult checkCompliance(ComplianceRequest request);
 *   }
 *
 */
@Service
public interface ComplianceNexusService {

    /**
     * Sync compliance check — validates amount, route, and regulatory risk.
     * The Compliance team's AI agent responds immediately with a risk decision.
     *
     * Called by: PaymentProcessingWorkflow (Payments side)
     * Handled by: ComplianceNexusServiceImpl (Compliance side)
     */
    @Operation
    ComplianceResult checkCompliance(ComplianceRequest request);
}
