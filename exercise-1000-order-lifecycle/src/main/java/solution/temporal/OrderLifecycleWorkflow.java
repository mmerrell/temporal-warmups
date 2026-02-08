package solution.temporal;

import exercise.domain.Order;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import solution.temporal.domain.OrderTrackingInfo;

@WorkflowInterface
public interface OrderLifecycleWorkflow {
    @WorkflowMethod
    String processOrder(Order order);

    @QueryMethod
    String getOrderStatus();
    @QueryMethod
    OrderTrackingInfo getTrackingInfo();
}