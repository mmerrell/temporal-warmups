// GreetingActivitiesImpl.java
package helloworld;

public class GreetingActivitiesImpl implements GreetingActivities {

    @Override
    public String getGreeting(String name) {
        System.out.println("Creating greeting for: " + name);
        return "Hello, " + name + "!";
    }

    @Override
    public String sendGreeting(String greeting) {
        System.out.println("Sending: " + greeting);
        return "Greeting sent successfully";
    }
}