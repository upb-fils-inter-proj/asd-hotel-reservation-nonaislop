package org.nightking.shop.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "shop_api_password=test-secret")
@AutoConfigureMockMvc
class ShopApiTest {
    private static final String QUOTE = """
            {"nightlyRate": 100.0, "nights": 3, "breakfast": true, "spa": true, "airportTransfer": false}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void requiresCredentials() throws Exception {
        mvc.perform(post("/api/shop/quotes").contentType(MediaType.APPLICATION_JSON).content(QUOTE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mvc.perform(post("/api/shop/quotes").with(httpBasic("shop-client", "wrong"))
                        .contentType(MediaType.APPLICATION_JSON).content(QUOTE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void quotesAStay() throws Exception {
        mvc.perform(post("/api/shop/quotes").with(httpBasic("shop-client", "test-secret"))
                        .contentType(MediaType.APPLICATION_JSON).content(QUOTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(450.0))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.breakdown").value("Room stay + Breakfast + Spa package"));
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/shop/quotes").with(httpBasic("shop-client", "test-secret"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nightlyRate\": 100.0, \"nights\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
