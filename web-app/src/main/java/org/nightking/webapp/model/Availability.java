package org.nightking.webapp.model;

import java.time.LocalDate;
import java.util.List;

/** The rooms free for every night of a stay. */
public record Availability(LocalDate checkIn, LocalDate checkOut, int nights, List<Room> rooms) {
}
