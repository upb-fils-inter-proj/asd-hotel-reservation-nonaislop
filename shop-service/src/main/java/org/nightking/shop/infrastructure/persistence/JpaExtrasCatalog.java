package org.nightking.shop.infrastructure.persistence;

import org.nightking.shop.entity.Extra;
import org.nightking.shop.gateway.ExtrasCatalog;

/** ExtrasCatalog gateway backed by the shop database. */
public class JpaExtrasCatalog implements ExtrasCatalog {
    private final ExtraPriceJpaRepository prices;

    public JpaExtrasCatalog(ExtraPriceJpaRepository prices) {
        this.prices = prices;
    }

    @Override
    public double priceOf(Extra extra) {
        return prices.findById(extra.name())
                .orElseThrow(() -> new IllegalStateException("No price configured for " + extra))
                .getPrice().doubleValue();
    }
}
