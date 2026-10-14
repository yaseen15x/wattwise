package com.yaseen.wattwise;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class ApplianceApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void supportsApplianceLifecycleWithinOneSession() throws Exception {
        Cookie visitor = newVisitor();
        String id = createAppliance(visitor);

        mvc.perform(get("/api/appliances").cookie(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(get("/api/appliances/{id}", id).cookie(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Heater"))
                .andExpect(jsonPath("$.powerWatts").value(2000));

        mvc.perform(post("/api/appliances/{id}/calculate", id)
                        .cookie(visitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationHours": 3,
                                  "rateGbpPerKwh": 0.25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyKwh").value(6))
                .andExpect(jsonPath("$.costGbp").value(1.50));

        mvc.perform(put("/api/appliances/{id}", id)
                        .cookie(visitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Small heater",
                                  "powerWatts": 1000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Small heater"));

        mvc.perform(get("/api/appliances/{id}", id).cookie(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.powerWatts").value(1000));

        mvc.perform(post("/api/appliances/{id}/calculate", id)
                        .cookie(visitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationHours": 3,
                                  "rateGbpPerKwh": 0.25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyKwh").value(3))
                .andExpect(jsonPath("$.costGbp").value(0.75));

        mvc.perform(delete("/api/appliances/{id}", id).cookie(visitor))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/appliances/{id}", id).cookie(visitor))
                .andExpect(status().isNotFound());
    }

    @Test
    void preventsAccessFromAnotherVisitor() throws Exception {
        Cookie owner = newVisitor();
        Cookie otherVisitor = newVisitor();
        String id = createAppliance(owner);

        mvc.perform(get("/api/appliances").cookie(otherVisitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/appliances/{id}", id)
                        .cookie(otherVisitor))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/appliances/{id}", id)
                        .cookie(otherVisitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Changed by another visitor",
                                  "powerWatts": 10
                                }
                                """))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/appliances/{id}/calculate", id)
                        .cookie(otherVisitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationHours": 3,
                                  "rateGbpPerKwh": 0.25
                                }
                                """))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/appliances/{id}", id)
                        .cookie(otherVisitor))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/appliances/{id}", id))
                .andExpect(status().isNotFound());

        // The owner's record must remain unchanged.
        mvc.perform(get("/api/appliances/{id}", id).cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Heater"))
                .andExpect(jsonPath("$.powerWatts").value(2000));
    }

    @Test
    void rejectsNegativePower() throws Exception {
        mvc.perform(post("/api/appliances")
                        .cookie(newVisitor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid appliance",
                                  "powerWatts": -1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundWhenCalculatingMissingAppliance() throws Exception {
        mvc.perform(post(
                        "/api/appliances/{id}/calculate",
                        UUID.randomUUID()
                )
                        .cookie(newVisitor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationHours": 3,
                                  "rateGbpPerKwh": 0.25
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    private Cookie newVisitor() throws Exception {
        Cookie cookie = mvc.perform(get("/api/appliances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0))
                .andReturn()
                .getResponse()
                .getCookie("WATTWISE_SESSION");

        assertNotNull(cookie, "The server must issue a visitor cookie");

        return cookie;
    }

    private String createAppliance(Cookie cookie) throws Exception {
        String body = mvc.perform(post("/api/appliances")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Heater",
                                  "powerWatts": 2000
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(body, "$.id");
    }
}