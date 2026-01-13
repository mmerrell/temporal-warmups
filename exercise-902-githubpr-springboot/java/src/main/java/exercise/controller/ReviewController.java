package exercise.controller;

import exercise.model.ReviewRequest;
import exercise.model.ReviewResponse;
import exercise.service.ReviewOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the GitHub PR review API.
 *
 * Exposes POST /review endpoint that accepts a PR and returns a review with
 * recommendations from three AI agents: Code Quality, Test Quality, and Security.
 */
@RestController
public class ReviewController {

    private final ReviewOrchestrator orchestrator;

    @Autowired
    public ReviewController(ReviewOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Reviews a GitHub pull request using three AI agents.
     *
     * WARNING: This endpoint blocks the HTTP thread for 6-9+ seconds while
     * the agents analyze the PR. This is intentionally bad architecture to
     * demonstrate the problems that Temporal solves.
     *
     * @param request The PR to review (title, description, diff, test summary)
     * @return ReviewResponse with overall recommendation and agent results
     */
    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> review(@RequestBody ReviewRequest request) {
        try {
            // Orchestrate the review (blocks for 6-9+ seconds!)
            ReviewResponse response = orchestrator.review(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Fail fast - return 500 error with no retry
            System.err.println("Review endpoint failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
