package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.model.ReservationResponse;

/** Checks the guest out and frees the room. Empty if the reservation does not exist. */
public interface CheckOutUseCase {
    Optional<ReservationResponse> execute(String reservationId);
}
