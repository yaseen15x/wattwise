package com.yaseen.wattwise;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

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
        MockHttpSession visitor = new MockHttpSession();
        String id = createAppliance(visitor);

        mvc.perform(get("/api/appliances").session(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(get("/api/appliances/{id}", id).session(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Heater"))
                .andExpect(jsonPath("$.powerWatts").value(2000));

        mvc.perform(post("/api/appliances/{id}/calculate", id)
                        .session(visitor)
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
                        .session(visitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Small heater",
                                  "powerWatts": 1000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Small heater"));

        mvc.perform(get("/api/appliances/{id}", id).session(visitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.powerWatts").value(1000));

        mvc.perform(post("/api/appliances/{id}/calculate", id)
                        .session(visitor)
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

        mvc.perform(delete("/api/appliances/{id}", id).session(visitor))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/appliances/{id}", id).session(visitor))
                .andExpect(status().isNotFound());
    }

    @Test
    void preventsAccessFromAnotherVisitor() throws Exception {
        MockHttpSession owner = new MockHttpSession();
        MockHttpSession otherVisitor = new MockHttpSession();
        String id = createAppliance(owner);

        mvc.perform(get("/api/appliances").session(otherVisitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/appliances/{id}", id)
                        .session(otherVisitor))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/appliances/{id}", id)
                        .session(otherVisitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Changed by another visitor",
                                  "powerWatts": 10
                                }
                                """))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/appliances/{id}/calculate", id)
                        .session(otherVisitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationHours": 3,
                                  "rateGbpPerKwh": 0.25
                                }
                                """))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/appliances/{id}", id)
                        .session(otherVisitor))
                .andExpect(status().isNotFound());

        // A request without the owner's session cannot access it either.
        mvc.perform(get("/api/appliances/{id}", id))
                .andExpect(status().isNotFound());

        // Failed attempts must not change or remove the owner's record.
        mvc.perform(get("/api/appliances/{id}", id).session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Heater"))
                .andExpect(jsonPath("$.powerWatts").value(2000));
    }

    @Test
    void rejectsNegativePower() throws Exception {
        mvc.perform(post("/api/appliances")
                        .session(new MockHttpSession())
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
                        .session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationHours": 3,
                                  "rateGbpPerKwh": 0.25
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    private String createAppliance(MockHttpSession session) throws Exception {
        String body = mvc.perform(post("/api/appliances")
                        .session(session)
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