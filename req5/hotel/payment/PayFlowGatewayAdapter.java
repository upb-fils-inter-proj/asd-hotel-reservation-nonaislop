package hotel.payment;

public class PayFlowGatewayAdapter implements PaymentProcessor {
    private final PayFlowGateway gateway;

    public PayFlowGatewayAdapter(PayFlowGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean processPayment(String reservationId, double amount) {
        int amountInCents = (int) Math.round(amount * 100);
        System.out.println("Adapter converts euros to cents and maps the gateway call.");
        return gateway.makeCharge(reservationId, amountInCents);
    }
}
