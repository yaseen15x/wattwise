package com.yaseen.wattwise;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    void createsCalculatesUpdatesAndDeletesAppliance() throws Exception {
        String response = mvc.perform(post("/api/appliances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Heater",
                                  "powerWatts": 2000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Heater"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(response, "$.id");
        String applianceUrl = "/api/appliances/" + id;

        // A separate request must retrieve the saved appliance.
        mvc.perform(get(applianceUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Heater"))
                .andExpect(jsonPath("$.powerWatts").value(2000));

        mvc.perform(post(applianceUrl + "/calculate")
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

        mvc.perform(put(applianceUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Small heater",
                                  "powerWatts": 1000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // Verify the update persisted beyond the PUT request.
        mvc.perform(get(applianceUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Small heater"))
                .andExpect(jsonPath("$.powerWatts").value(1000));

        // Calculations must now use the updated power.
        mvc.perform(post(applianceUrl + "/calculate")
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

        mvc.perform(delete(applianceUrl))
                .andExpect(status().isNoContent());

        mvc.perform(get(applianceUrl))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsNegativePower() throws Exception {
        mvc.perform(post("/api/appliances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid heater",
                                  "powerWatts": -100
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotCalculateForMissingAppliance() throws Exception {
        mvc.perform(post(
                        "/api/appliances/"
                                + "00000000-0000-0000-0000-000000000000"
                                + "/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationHours": 3,
                                  "rateGbpPerKwh": 0.25
                                }
                                """))
                .andExpect(status().isNotFound());
    }
}