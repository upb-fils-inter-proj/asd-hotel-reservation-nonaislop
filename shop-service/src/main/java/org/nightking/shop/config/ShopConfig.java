package org.nightking.shop.config;

import org.nightking.shop.entity.pricing.PricingPolicy;
import org.nightking.shop.entity.pricing.SeasonalPricing;
import org.nightking.shop.gateway.ExtrasCatalog;
import org.nightking.shop.infrastructure.persistence.ExtraPriceJpaRepository;
import org.nightking.shop.infrastructure.persistence.JpaExtrasCatalog;
import org.nightking.shop.usecase.QuoteStayInteractor;
import org.nightking.shop.usecase.QuoteStayUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root of the shop service. */
@Configuration
public class ShopConfig {

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
}
