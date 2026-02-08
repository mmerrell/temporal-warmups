package solution.temporal;

import exercise.domain.Order;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Starter {
    //1. set a task queue name in an interface for reuse

    //2. create main
    public static void main(String[] args) {
        //3. connect to temporal server
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        //4. create workflowId
        String workflowId = SharedData.TASK_QUEUE + "-" + UUID.randomUUID();
        //5. create a workflow stub
        OrderLifecycleWorkflow workflow = client.newWorkflowStub(OrderLifecycleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(SharedData.TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        //6. call the workflow
        List<Order> orders = List.of(
                new Order("ORD-001", "alice@example.com", "123 Main St, Springfield, IL 62701",
                        List.of(
                                new Order.OrderItem("LAPTOP-001", "Pro Laptop 15\"", 1, 1299.99),
                                new Order.OrderItem("MOUSE-002", "Wireless Mouse", 2, 29.99)
                        )),
                new Order("ORD-002", "bob@example.com", "456 Oak Ave, Portland, OR 97201",
                        List.of(
                                new Order.OrderItem("MONITOR-004", "27\" 4K Monitor", 2, 499.99),
                                new Order.OrderItem("KEYBOARD-003", "Mechanical Keyboard", 1, 149.99)
                        )),
                new Order("ORD-003", "carol@example.com", "789 Pine Rd, Austin, TX 78701",
                        List.of(
                                new Order.OrderItem("HEADSET-005", "Noise-Canceling Headset", 3, 199.99)
                        ))
        );

        Map<String, String> results = new LinkedHashMap<>();

        for (Order order : orders) {
            String result = workflow.processOrder(order);
            results.put(order.orderId, result);
        }
    }
}
