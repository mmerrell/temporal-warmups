// Worker.java
package helloworld;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;

public class Worker {

    public static void main(String[] args) {
        // Connect to Temporal
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Create worker for task queue
        io.temporal.worker.Worker worker = factory.newWorker("greeting-queue");

        // Register workflow implementation
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

        // Register activity implementation
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

        // Start worker
        factory.start();

        System.out.println("Worker started on task queue: greeting-queue");
    }
}