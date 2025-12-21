// GreetingWorkflowImpl.java
package helloworld;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class GreetingWorkflowImpl implements GreetingWorkflow {

    // Create activity stub
    private final GreetingActivities activities =
            Workflow.newActivityStub(
                    GreetingActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(10))
                            .build()
            );

    @Override
    public String greet(String name) {
        // Call activities in sequence
        String greeting = activities.getGreeting(name);
        String result = activities.sendGreeting(greeting);
        return result;
    }
}