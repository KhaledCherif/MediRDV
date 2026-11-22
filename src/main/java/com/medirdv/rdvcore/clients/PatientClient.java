package com.medirdv.rdvcore.clients;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.medirdv.rdvcore.dto.PatientDto;
import com.medirdv.rdvcore.exceptions.PatientNotFoundException;
import com.medirdv.rdvcore.exceptions.PatientServiceUnavailableException;

/**
 * REST client toward patient-service (issue #24).
 */
@Component
public class PatientClient {

    private final RestClient patientRestClient;

    public PatientClient(RestClient patientRestClient) {
        this.patientRestClient = patientRestClient;
    }

    /**
     * Fetches a patient by id.
     *
     * @throws PatientNotFoundException           if patient-service answers 404
     * @throws PatientServiceUnavailableException if patient-service is down, times out or answers 5xx
     */
    public PatientDto getPatient(Long patientId) {
        try {
            return patientRestClient.get()
                    .uri("/api/v1/patients/{id}", patientId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PatientDto.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new PatientNotFoundException(patientId);
            }
            throw e;
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new PatientServiceUnavailableException("patient-service is unavailable", e);
        }
    }

    /**
     * Convenience check used before creating a rendezvous.
     */
    public void assertPatientExists(Long patientId) {
        getPatient(patientId);
    }
}
