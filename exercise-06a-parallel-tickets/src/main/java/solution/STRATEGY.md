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

## 4. Create the Starter using 8-step process

1. create a task queue name
2. create a main()
3. connect to temporal server just like in the Worker
4. create a for loop to iterate over the tickets
5. create unique workflowId
6. create Workflow stub using `client.newWorkflowStub()`
7. run the workflow
8. process results

## 5. Create Worker

This is the more involved step. It will require creating the `ActivityImpl` and the business logic objects.

## 6. Update POM.xml for simple CLI execution

Add this to the POM.xml.

💡 If we keep the same class names then we can just do a simple copy and paste

```xml
            <!-- Exec plugin for running main classes -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.0.0</version>
                <configuration>
                    <!-- Default: run pre-temporal baseline -->
                    <mainClass>exercise.SupportTriageService</mainClass>
                </configuration>
<!--                Add an <execution> to run workflow and worker
                    If we keep the same class names, it's just a copy and paste -->
                <executions>
                    <execution>
                        <id>worker</id>
                        <configuration>
                            <mainClass>solution.temporal.WorkerApp</mainClass>
                        </configuration>
                    </execution>
                    <execution>
                        <id>workflow</id>
                        <configuration>
                            <mainClass>solution.temporal.Starter</mainClass>
                        </configuration>
                    </execution>
                </executions>
            </plugin>


```
   


