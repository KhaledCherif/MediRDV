package com.medirdv.rdvcore.dto;

import java.util.UUID;

import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;

public record RendezvousResponse(
        Long id,
        Long patientId,
        Long creneauId,
        RendezvousStatus statut,
        UUID requestId) {

    public static RendezvousResponse from(Rendezvous rendezvous) {
        return new RendezvousResponse(
                rendezvous.getId(),
                rendezvous.getPatientId(),
                rendezvous.getCreneauId(),
                rendezvous.getStatut(),
                rendezvous.getRequestId());
    }
}
