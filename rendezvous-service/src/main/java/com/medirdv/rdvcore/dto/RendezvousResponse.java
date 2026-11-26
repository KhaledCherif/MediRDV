package com.medirdv.rdvcore.dto;

import java.util.UUID;

import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;

public record RendezvousResponse(
        Long id,
        Long patientId,
        Long creneauId,
        Long praticienId,
        RendezvousStatus statut,
        UUID requestId,
        boolean pendingDeclaration,
        boolean pendingLiberation) {

    public static RendezvousResponse from(Rendezvous rendezvous) {
        return new RendezvousResponse(
                rendezvous.getId(),
                rendezvous.getPatientId(),
                rendezvous.getCreneauId(),
                rendezvous.getPraticienId(),
                rendezvous.getStatut(),
                rendezvous.getRequestId(),
                rendezvous.isPendingDeclaration(),
                rendezvous.isPendingLiberation());
    }
}
