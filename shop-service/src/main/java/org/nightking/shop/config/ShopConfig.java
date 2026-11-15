package org.nightking.shop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.nightking.shop.controller.ShopMessageController;
import org.nightking.shop.entity.pricing.PricingPolicy;
import org.nightking.shop.entity.pricing.SeasonalPricing;
import org.nightking.shop.gateway.ExtrasCatalog;
import org.nightking.shop.infrastructure.messaging.MqttResponder;
import org.nightking.shop.infrastructure.messaging.MqttTransport;
import org.nightking.shop.infrastructure.messaging.PahoMqttTransport;
import org.nightking.shop.infrastructure.persistence.ExtraPriceJpaRepository;
import org.nightking.shop.infrastructure.persistence.JpaExtrasCatalog;
import org.nightking.shop.usecase.QuoteStayInteractor;
import org.nightking.shop.usecase.QuoteStayUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root of the shop service. */
@Configuration
public class ShopConfig {
    /** Quote requests, shared between all running shop instances (each request goes to one of them). */
    static final String QUOTE_REQUESTS = "$share/shop-service/shop/quotes/request";

    @Bean
    PricingPolicy pricingPolicy() {
        // Select the pricing Strategy. Alternatives:
        // new StandardPricing()
        // new LongStayDiscountPricing(5, 0.10)
        return new PricingPolicy(new SeasonalPricing(1.20));
    }

    @Bean
    ExtrasCatalog extrasCatalog(ExtraPriceJpaRepository prices) {
        return new JpaExtrasCatalog(prices);
    }

    @Bean
    QuoteStayUseCase quoteStayUseCase(PricingPolicy pricingPolicy, ExtrasCatalog extrasCatalog) {
        return new QuoteStayInteractor(pricingPolicy, extrasCatalog);
    }

    // --- the MQTT side: the hotel management service asks for quotes through the broker

    @Bean
    MqttTransport mqttTransport(@Value("${messaging.mqtt.url}") String url,
                                @Value("${messaging.mqtt.username}") String username,
                                @Value("${messaging.mqtt.password}") String password) {
        PahoMqttTransport transport = new PahoMqttTransport(url,
                "shop-" + UUID.randomUUID().toString().substring(0, 8), username, password);
        transport.start();
        return transport;
    }

    @Bean
    ShopMessageController shopMessageController(QuoteStayUseCase quoteStay, ObjectMapper json) {
        return new ShopMessageController(quoteStay, json);
    }

    @Bean
    ApplicationRunner answerQuoteRequests(MqttTransport transport, ShopMessageController controller) {
        return args -> new MqttResponder(transport).serve(QUOTE_REQUESTS, controller::quoteStay);
    }
}
