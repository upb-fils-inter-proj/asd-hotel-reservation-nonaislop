package org.nightking.hotelmanagement.presenter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.nightking.hotelmanagement.model.RoomResponse;

public record RoomViewModel(String roomNumber, String roomType, BigDecimal nightlyRate, String currency, String status) {

    static RoomViewModel from(RoomResponse room) {
        return new RoomViewModel(room.roomNumber(), room.roomType(),
                BigDecimal.valueOf(room.nightlyRate()).setScale(2, RoundingMode.HALF_UP), "EUR", room.status());
    }
}
