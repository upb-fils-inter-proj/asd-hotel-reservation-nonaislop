package org.nightking.shop.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "extra_prices")
public class ExtraPriceJpaEntity {
    @Id
    private String code;
    private BigDecimal price;

    protected ExtraPriceJpaEntity() {
    }

    BigDecimal getPrice() { return price; }
}
