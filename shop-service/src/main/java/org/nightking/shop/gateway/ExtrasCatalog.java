package org.nightking.shop.gateway;

import org.nightking.shop.entity.Extra;

/** Current price list of the additional services, in EUR. */
public interface ExtrasCatalog {
    double priceOf(Extra extra);
}
