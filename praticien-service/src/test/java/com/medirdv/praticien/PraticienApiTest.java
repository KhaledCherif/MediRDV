package com.medirdv.praticien;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PraticienApiTest {

    @Autowired MockMvc mvc;

    @Test
    void creerPraticienRenvoie201EtLocation() throws Exception {
        mvc.perform(post("/api/v1/praticiens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dr Ben Ali\",\"specialite\":\"Dermatologie\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void validationRenvoie400() throws Exception {
        mvc.perform(post("/api/v1/praticiens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"specialite\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void praticienInconnuRenvoie404() throws Exception {
        mvc.perform(get("/api/v1/praticiens/424242")).andExpect(status().isNotFound());
    }

    @Test
    void chevauchementRenvoie409() throws Exception {
        String body = mvc.perform(post("/api/v1/praticiens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dr Z\",\"specialite\":\"ORL\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        String slot = "{\"debut\":\"2026-12-02T09:00:00\",\"fin\":\"2026-12-02T09:30:00\"}";
        mvc.perform(post("/api/v1/praticiens/" + id + "/creneaux").contentType(MediaType.APPLICATION_JSON).content(slot))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/praticiens/" + id + "/creneaux").contentType(MediaType.APPLICATION_JSON).content(slot))
                .andExpect(status().isConflict());
    }

    @Test
    void actuatorHealthUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
