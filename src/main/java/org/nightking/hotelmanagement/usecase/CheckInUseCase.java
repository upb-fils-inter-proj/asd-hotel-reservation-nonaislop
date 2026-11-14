package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.model.ReservationResponse;

/** Checks the guest in and marks the room occupied. Empty if the reservation does not exist. */
public interface CheckInUseCase {
    Optional<ReservationResponse> execute(String reservationId);
}
