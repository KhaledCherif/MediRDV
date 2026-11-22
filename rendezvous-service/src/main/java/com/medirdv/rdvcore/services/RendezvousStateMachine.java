
package com.medirdv.rdvcore.services;

import org.springframework.stereotype.Service;

import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;

@Service
public class RendezvousStateMachine {

    public void transitionTo(
            Rendezvous rendezvous,
            RendezvousStatus newStatus) {

        if (rendezvous == null || newStatus == null) {
            throw new IllegalArgumentException(
                "Rendezvous and new status must not be null"
            );
        }

        RendezvousStatus currentStatus = rendezvous.getStatut();

        if (currentStatus == null) {
            throw new IllegalStateException(
                "Current rendezvous status is not defined"
            );
        }

        boolean allowed = switch (currentStatus) {

            case PLANIFIE ->
                newStatus == RendezvousStatus.ANNULE
                || newStatus == RendezvousStatus.CLOTURE;

            case DROITS_A_VERIFIER ->
                newStatus == RendezvousStatus.PLANIFIE
                || newStatus == RendezvousStatus.ANNULE;

            case ANNULE, CLOTURE -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                "Invalid transition from " + currentStatus
                + " to " + newStatus
            );
        }

        rendezvous.setStatut(newStatus);
    }
}
