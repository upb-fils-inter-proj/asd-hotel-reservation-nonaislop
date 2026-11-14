package org.nightking.webapp.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.nightking.webapp.model.BookingRequest;
import org.nightking.webapp.model.Registration;
import org.nightking.webapp.model.Reservation;
import org.nightking.webapp.model.Room;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

/** Calls the hotel management REST API with the web app's HTTP Basic credentials. */
public class HotelManagementClient implements HotelManagementGateway {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient rest;

    public HotelManagementClient(RestClient.Builder builder, String baseUrl, String username, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "hotel.password is empty: provide the hotel_api_password secret (see README).");
        }
        this.rest = builder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new HotelApiException(errorMessage(response));
                })
                .build();
    }

    @Override
    public List<Room> rooms() {
        return rest.get().uri("/api/rooms").retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Reservation> allReservations() {
        return rest.get().uri("/api/reservations").retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Reservation> reservationsOf(String guestId) {
        return rest.get().uri(uri -> uri.path("/api/reservations").queryParam("guestId", guestId).build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public Reservation book(BookingRequest booking) {
        return rest.post().uri("/api/reservations").contentType(MediaType.APPLICATION_JSON).body(booking)
                .retrieve().body(Reservation.class);
    }

    @Override
    public Reservation checkIn(String reservationId) {
        return rest.post().uri("/api/reservations/{id}/check-in", reservationId).retrieve().body(Reservation.class);
    }

    @Override
    public Reservation checkOut(String reservationId) {
        return rest.post().uri("/api/reservations/{id}/check-out", reservationId).retrieve().body(Reservation.class);
    }

    @Override
    public Account register(Registration registration) {
        return rest.post().uri("/api/accounts").contentType(MediaType.APPLICATION_JSON).body(registration)
                .retrieve().body(Account.class);
    }

    @Override
    public Optional<Account> authenticate(String username, String password) {
        return rest.post().uri("/api/accounts/authenticate").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .exchange((request, response) -> {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                        return Optional.empty();
                    }
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new HotelApiException(errorMessage(response));
                    }
                    return Optional.ofNullable(response.bodyTo(Account.class));
                });
    }

    private static String errorMessage(ClientHttpResponse response) throws IOException {
        try {
            JsonNode body = JSON.readTree(response.getBody());
            if (body != null && body.hasNonNull("error")) {
                return body.get("error").asText();
            }
        } catch (IOException ignored) {
            // fall through to a generic message
        }
        return "The hotel service refused the request (" + response.getStatusCode().value() + ").";
    }
}
