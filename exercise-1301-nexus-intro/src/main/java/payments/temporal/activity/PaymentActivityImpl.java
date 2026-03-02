package payments.temporal.activity;

import payments.PaymentGateway;
import payments.domain.PaymentRequest;

// The activity doesn't know about the business rules. Clean separation.
public class PaymentActivityImpl implements PaymentActivity{
    private PaymentGateway paymentGateway;

    public PaymentActivityImpl(PaymentGateway gateway){
        paymentGateway = gateway;
    }

    @Override
    public boolean validatePayment(PaymentRequest request) {
        return paymentGateway.validatePayment(request);
    }

    @Override
    public String executePayment(PaymentRequest request) {
        return paymentGateway.executePayment(request);
    }
}
