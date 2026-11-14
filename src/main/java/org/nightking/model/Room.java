package org.nightking.model;

public class Room {
    private final String roomNumber;
    private final String roomType;
    private final double nightlyRate;
    private RoomStatus status;

    public Room(String roomNumber, String roomType, double nightlyRate) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
        this.status = RoomStatus.AVAILABLE;
    }

    public String getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public double getNightlyRate() { return nightlyRate; }
    public RoomStatus getStatus() { return status; }

    public void updateStatus(RoomStatus status) {
        this.status = status;
        System.out.println("Room " + roomNumber + " status -> " + status);
    }
}
