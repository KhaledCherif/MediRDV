package com.medirdv.rdvcore.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.medirdv.rdvcore.dto.PatientDto;
import com.medirdv.rdvcore.exceptions.PatientNotFoundException;
import com.medirdv.rdvcore.exceptions.PatientServiceUnavailableException;

class PatientClientTest {

    private static final String BASE_URL = "http://patient-service:8081";

    private MockRestServiceServer server;
    private PatientClient patientClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        patientClient = new PatientClient(builder.build());
    }

    @Test
    void returnsPatientWhenFound() {
        server.expect(requestTo(BASE_URL + "/api/v1/patients/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":1,\"nom\":\"Ali\",\"contact\":\"ali@mail.tn\",\"numeroAssure\":\"CNAM-1\"}",
                        MediaType.APPLICATION_JSON));

        PatientDto patient = patientClient.getPatient(1L);

        assertThat(patient.id()).isEqualTo(1L);
        assertThat(patient.numeroAssure()).isEqualTo("CNAM-1");
        server.verify();
    }

    @Test
    void throwsNotFoundWhenPatientServiceReturns404() {
        server.expect(requestTo(BASE_URL + "/api/v1/patients/99"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> patientClient.getPatient(99L))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void throwsUnavailableWhenPatientServiceReturns5xx() {
        server.expect(requestTo(BASE_URL + "/api/v1/patients/1"))
                .andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> patientClient.getPatient(1L))
                .isInstanceOf(PatientServiceUnavailableException.class);
    }
}
