package com.medirdv.praticien;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.medirdv.praticien.service.CreneauService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "grpc.server.port=0"
})
class PraticienApiTest {

    @Autowired MockMvc mvc;
    @Autowired CreneauService creneaux;

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
    void creneauLibrePeutEtreModifieEtSupprime() throws Exception {
        String praticien = mvc.perform(post("/api/v1/praticiens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dr CRUD\",\"specialite\":\"ORL\"}"))
                .andReturn().getResponse().getContentAsString();
        String praticienId = praticien.replaceAll(".*\"id\":(\\d+).*", "$1");
        String slot = mvc.perform(post("/api/v1/praticiens/" + praticienId + "/creneaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"debut\":\"2027-01-02T09:00:00\",\"fin\":\"2027-01-02T09:30:00\"}"))
                .andReturn().getResponse().getContentAsString();
        String creneauId = slot.replaceAll(".*\"id\":(\\d+).*", "$1");
        String route = "/api/v1/praticiens/" + praticienId + "/creneaux/" + creneauId;

        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"debut\":\"2027-01-02T10:00:00\",\"fin\":\"2027-01-02T10:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debut").value("2027-01-02T10:00:00"));
        mvc.perform(post("/api/v1/praticiens/" + praticienId + "/creneaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"debut\":\"2027-01-02T10:45:00\",\"fin\":\"2027-01-02T11:15:00\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put(route).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"debut\":\"2027-01-02T10:30:00\",\"fin\":\"2027-01-02T11:00:00\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete(route)).andExpect(status().isNoContent());
        mvc.perform(get(route)).andExpect(status().isNotFound());
    }

    @Test
    void praticienPeutAccepterOuRefuserUneReservation() throws Exception {
        String praticien = mvc.perform(post("/api/v1/praticiens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dr Decision\",\"specialite\":\"ORL\"}"))
                .andReturn().getResponse().getContentAsString();
        String praticienId = praticien.replaceAll(".*\"id\":(\\d+).*", "$1");
        String creneauId = mvc.perform(post("/api/v1/praticiens/" + praticienId + "/creneaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"debut\":\"2027-02-02T09:00:00\",\"fin\":\"2027-02-02T09:30:00\"}"))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");

        creneaux.reserver(Long.valueOf(creneauId));
        mvc.perform(put("/api/v1/praticiens/" + praticienId + "/creneaux/" + creneauId + "/accepter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RESERVE"));

        String secondCreneau = mvc.perform(post("/api/v1/praticiens/" + praticienId + "/creneaux")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"debut\":\"2027-02-02T10:00:00\",\"fin\":\"2027-02-02T10:30:00\"}"))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");
        creneaux.reserver(Long.valueOf(secondCreneau));
        mvc.perform(put("/api/v1/praticiens/" + praticienId + "/creneaux/" + secondCreneau + "/refuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("LIBRE"));
    }

    @Test
    void actuatorHealthUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
