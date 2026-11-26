package com.medirdv.rdvcore.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.medirdv.rdvcore.exceptions.PraticienServiceUnavailableException;

class PraticienMetadataClientTest {
    private static final String BASE_URL = "http://praticien-service:8082";
    private MockRestServiceServer server;
    private PraticienMetadataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PraticienMetadataClient(builder.build());
    }

    @Test
    void readsRealSlotDateAndPractitionerName() {
        server.expect(requestTo(BASE_URL + "/api/v1/praticiens/7"))
                .andRespond(withSuccess("{\"id\":7,\"nom\":\"Dr A\",\"specialite\":\"cardio\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/v1/praticiens/7/creneaux/10"))
                .andRespond(withSuccess("{\"id\":10,\"praticienId\":7,\"debut\":\"2026-11-26T10:00:00\",\"fin\":\"2026-11-26T10:30:00\"}",
                        MediaType.APPLICATION_JSON));

        var metadata = client.consultation(7L, 10L);

        assertThat(metadata.date()).isEqualTo(LocalDateTime.of(2026, 11, 26, 10, 0));
        assertThat(metadata.praticien()).isEqualTo("Dr A");
        server.verify();
    }

    @Test
    void rejectsUnavailablePractitionerService() {
        server.expect(requestTo(BASE_URL + "/api/v1/praticiens/7"))
                .andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> client.consultation(7L, 10L))
                .isInstanceOf(PraticienServiceUnavailableException.class);
    }
}
