package com.medirdv.rdvcore.clients;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.medirdv.rdvcore.exceptions.PraticienServiceUnavailableException;

/** Reads consultation facts from the owner of practitioner and slot data. */
@Component
public class PraticienMetadataClient {
    private final RestClient restClient;

    public PraticienMetadataClient(@Qualifier("praticienMetadataRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ConsultationMetadata consultation(Long praticienId, Long creneauId) {
        try {
            PraticienInfo praticien = restClient.get()
                    .uri("/api/v1/praticiens/{id}", praticienId)
                    .retrieve().body(PraticienInfo.class);
            CreneauInfo creneau = restClient.get()
                    .uri("/api/v1/praticiens/{id}/creneaux/{creneauId}", praticienId, creneauId)
                    .retrieve().body(CreneauInfo.class);
            if (praticien == null || creneau == null
                    || !praticienId.equals(praticien.id())
                    || !creneauId.equals(creneau.id())
                    || !praticienId.equals(creneau.praticienId())
                    || praticien.nom() == null || praticien.nom().isBlank()
                    || creneau.debut() == null) {
                throw new PraticienServiceUnavailableException("praticien-service returned incomplete consultation data", null);
            }
            return new ConsultationMetadata(creneau.debut(), praticien.nom());
        } catch (RestClientException e) {
            throw new PraticienServiceUnavailableException("praticien-service consultation data unavailable", e);
        }
    }

    public record ConsultationMetadata(LocalDateTime date, String praticien) {}
    public record PraticienInfo(Long id, String nom) {}
    public record CreneauInfo(Long id, Long praticienId, LocalDateTime debut) {}
}
