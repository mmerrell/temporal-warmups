package shared.nexus;

import compliance.domain.CategoryRequest;
import compliance.domain.RiskScreeningRequest;
import compliance.domain.RiskScreeningResult;
import compliance.domain.TransactionCategory;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

/**
 * [STUDENT IMPLEMENTS] Nexus Service Interface - the shared contract between teams.
 *
 * This is the KEY new concept in this exercise. Think of it as:
 * - A durable, type-safe API contract between Payments and Compliance teams
 * - Like a REST API definition, but with Temporal's durability guarantees
 * - Both teams depend on this interface (shared library / shared package)
 *
 * Annotations:
 * - @Service (io.nexusrpc) marks this as a Nexus service interface
 * - @Operation marks each method as a Nexus operation
 *
 * Two operations:
 * 1. screenTransaction() - ASYNC: Starts a fraud detection workflow (long-running)
 * 2. categorizeTransaction() - SYNC: Quick categorization (returns immediately)
 */
@Service
public interface ComplianceNexusService {

    /**
     * Async operation - triggers a full fraud detection workflow on the Compliance side.
     * The caller gets a handle and can wait for the result.
     * Maps to: FraudDetectionWorkflow.screenTransaction()
     */
    @Operation
    RiskScreeningResult screenTransaction(RiskScreeningRequest request);

    /**
     * Sync operation - quick AI categorization, returns immediately.
     * Useful for lightweight operations that don't need a full workflow.
     */
    @Operation
    TransactionCategory categorizeTransaction(CategoryRequest request);
}
