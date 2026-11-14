package hotel.payment;

public class PayFlowGateway {
    public boolean makeCharge(String bookingReference, int amountInCents) {
        System.out.println("[PayFlow] Charging " + amountInCents
                + " cents for booking " + bookingReference);
        return amountInCents > 0;
    }
}
