package org.nightking.webapp.config;

import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.infrastructure.HotelManagementClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composition root of the web app. */
@Configuration
public class WebAppConfig {

    @Bean
    HotelManagementGateway hotelManagementGateway(RestClient.Builder builder,
                                                  @Value("${hotel.url}") String url,
                                                  @Value("${hotel.username}") String username,
                                                  @Value("${hotel.password}") String password) {
        return new HotelManagementClient(builder.clone(), url, username, password);
    }
}
