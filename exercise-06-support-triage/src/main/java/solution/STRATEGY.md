# Strategy for Solving

## Problems with the traditional approach

1. No retries for LLM API failures - if something fails, you may need to rerun the whole workflow again, costing you time and $
2. No visibility into the multi-agent decision process. We need to code that in. Temporal provides this out of the box.
3. No audit trail of AI decisions. Need to code in. Temporal is provided by default.
4. No human-in-the-loop approval

## 1. Activities
The activities here include:
1. `scrubPII(String ticketText)` because this makes an API call with an LLM
2. `classifyTicket(String scrubbedText)` another external LLM

## 2. Workflow

```
Activity 1 (PII Scrubber) → returns scrubbed text
                         ↓
Activity 2 (Classifier) → takes scrubbed text, returns classification
                         ↓
Workflow Logic → makes routing decision (needs human review?)
```

1. `String scrubbedText = scrubPII(ticketText);`
2. `TicketClassification classification = classifyTicket(scrubbedText);`
3. `boolean needsHumanReview =
                classification.confidence < 0.7 ||
                classification.urgency.equals("critical");`
   Question: How do we make this real?
5. Create CRM Case
This is simply logging to the console.
However, if we wanted to add a real call to the workflow, we would replace the logging with an actual implementation.

Output:
`new TriageResult(true, ticketId, classification, caseId, null, needsHumanReview);`

## 3. There's a for loop for the tickets, where does that go?
Into `Starter.java` that passes the information into the workflow, which will take in the `ticketText` and then follow the steps
