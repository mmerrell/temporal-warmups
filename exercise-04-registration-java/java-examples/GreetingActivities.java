// GreetingActivities.java
package helloworld;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface GreetingActivities {

    @ActivityMethod
    String getGreeting(String name);

    @ActivityMethod
    String sendGreeting(String greeting);
}