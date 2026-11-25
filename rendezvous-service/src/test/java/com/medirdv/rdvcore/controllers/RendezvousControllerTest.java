package com.medirdv.rdvcore.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;
import com.medirdv.rdvcore.exceptions.PatientNotFoundException;
import com.medirdv.rdvcore.exceptions.PatientServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.RendezvousNotFoundException;
import com.medirdv.rdvcore.services.RendezvousService;

@WebMvcTest(RendezvousController.class)
class RendezvousControllerTest {

    private static final UUID REQUEST_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RendezvousService rendezvousService;

    private static Rendezvous rendezvous(RendezvousStatus statut) {
        Rendezvous r = new Rendezvous(1L, 10L, statut, REQUEST_ID);
        ReflectionTestUtils.setField(r, "id", 5L);
        return r;
    }

    private static String createBody(String patientId) {
        return "{\"patientId\":" + patientId + ",\"creneauId\":10,\"requestId\":\"" + REQUEST_ID + "\"}";
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(rendezvousService.create(any()))
                .thenReturn(new RendezvousService.CreationResult(rendezvous(RendezvousStatus.DROITS_A_VERIFIER), true));

        mockMvc.perform(post("/api/v1/rendezvous").contentType(MediaType.APPLICATION_JSON).content(createBody("1")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/rendezvous/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.statut").value("DROITS_A_VERIFIER"));
    }

    @Test
    void createReplayReturns200() throws Exception {
        when(rendezvousService.create(any()))
                .thenReturn(new RendezvousService.CreationResult(rendezvous(RendezvousStatus.PLANIFIE), false));

        mockMvc.perform(post("/api/v1/rendezvous").contentType(MediaType.APPLICATION_JSON).content(createBody("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PLANIFIE"));
    }

    @Test
    void createWithReusedRequestIdForDifferentBookingReturns409() throws Exception {
        when(rendezvousService.create(any()))
                .thenThrow(new IllegalStateException("requestId already belongs to a different rendezvous"));

        mockMvc.perform(post("/api/v1/rendezvous").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("1")))
                .andExpect(status().isConflict());
    }

    @Test
    void createWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/rendezvous").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.patientId").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void createWithUnknownPatientReturns404() throws Exception {
        when(rendezvousService.create(any())).thenThrow(new PatientNotFoundException(42L));

        mockMvc.perform(post("/api/v1/rendezvous").contentType(MediaType.APPLICATION_JSON).content(createBody("42")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    void createWhenPatientServiceDownReturns503() throws Exception {
        when(rendezvousService.create(any()))
                .thenThrow(new PatientServiceUnavailableException("patient-service is unavailable", null));

        mockMvc.perform(post("/api/v1/rendezvous").contentType(MediaType.APPLICATION_JSON).content(createBody("1")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getUnknownReturns404() throws Exception {
        when(rendezvousService.findById(99L)).thenThrow(new RendezvousNotFoundException(99L));

        mockMvc.perform(get("/api/v1/rendezvous/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        when(rendezvousService.changeStatut(eq(5L), eq(RendezvousStatus.PLANIFIE)))
                .thenThrow(new IllegalStateException("Invalid transition from ANNULE to PLANIFIE"));

        mockMvc.perform(patch("/api/v1/rendezvous/5/statut").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"PLANIFIE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownStatutValueReturns400() throws Exception {
        mockMvc.perform(patch("/api/v1/rendezvous/5/statut").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"N_IMPORTE_QUOI\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validTransitionReturns200() throws Exception {
        when(rendezvousService.changeStatut(5L, RendezvousStatus.ANNULE))
                .thenReturn(rendezvous(RendezvousStatus.ANNULE));

        mockMvc.perform(patch("/api/v1/rendezvous/5/statut").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"ANNULE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULE"));
    }
}
