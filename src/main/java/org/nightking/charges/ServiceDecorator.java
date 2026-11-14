package org.nightking.charges;

public abstract class ServiceDecorator implements Chargeable {
    protected final Chargeable wrappedCharge;

    protected ServiceDecorator(Chargeable wrappedCharge) {
        this.wrappedCharge = wrappedCharge;
    }
}
