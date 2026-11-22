package com.medirdv.rdvcore.services;

import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RendezvousStateMachineTest {

    private RendezvousStateMachine stateMachine;
    private Rendezvous rendezvous;

    @BeforeEach
    void setUp() {
        stateMachine = new RendezvousStateMachine();
        rendezvous = new Rendezvous();
    }

    @Test
    void shouldCancelPlannedAppointment() {
        rendezvous.setStatut(RendezvousStatus.PLANIFIE);

        stateMachine.transitionTo(rendezvous, RendezvousStatus.ANNULE);

        assertEquals(RendezvousStatus.ANNULE, rendezvous.getStatut());
    }

    @Test
    void shouldClosePlannedAppointment() {
        rendezvous.setStatut(RendezvousStatus.PLANIFIE);

        stateMachine.transitionTo(rendezvous, RendezvousStatus.CLOTURE);

        assertEquals(RendezvousStatus.CLOTURE, rendezvous.getStatut());
    }

    @Test
    void shouldConfirmAppointmentAfterRightsVerification() {
        rendezvous.setStatut(RendezvousStatus.DROITS_A_VERIFIER);

        stateMachine.transitionTo(rendezvous, RendezvousStatus.PLANIFIE);

        assertEquals(RendezvousStatus.PLANIFIE, rendezvous.getStatut());
    }

    @Test
    void shouldCancelAppointmentWithPendingRights() {
        rendezvous.setStatut(RendezvousStatus.DROITS_A_VERIFIER);

        stateMachine.transitionTo(rendezvous, RendezvousStatus.ANNULE);

        assertEquals(RendezvousStatus.ANNULE, rendezvous.getStatut());
    }

    @Test
    void shouldRejectTransitionFromCancelledAppointment() {
        rendezvous.setStatut(RendezvousStatus.ANNULE);

        assertThrows(IllegalStateException.class,
            () -> stateMachine.transitionTo(rendezvous, RendezvousStatus.PLANIFIE));
    }

    @Test
    void shouldRejectTransitionFromClosedAppointment() {
        rendezvous.setStatut(RendezvousStatus.CLOTURE);

        assertThrows(IllegalStateException.class,
            () -> stateMachine.transitionTo(rendezvous, RendezvousStatus.ANNULE));
    }
}
