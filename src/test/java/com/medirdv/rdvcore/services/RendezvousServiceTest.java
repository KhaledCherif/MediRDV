package com.medirdv.rdvcore.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medirdv.rdvcore.clients.PatientClient;
import com.medirdv.rdvcore.dto.CreateRendezvousRequest;
import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;
import com.medirdv.rdvcore.exceptions.PatientNotFoundException;
import com.medirdv.rdvcore.exceptions.RendezvousNotFoundException;
import com.medirdv.rdvcore.repositories.RendezvousRepository;

@ExtendWith(MockitoExtension.class)
class RendezvousServiceTest {

    private static final UUID REQUEST_ID = UUID.randomUUID();

    @Mock
    private RendezvousRepository repository;

    @Mock
    private PatientClient patientClient;

    private RendezvousService service;

    @BeforeEach
    void setUp() {
        service = new RendezvousService(repository, new RendezvousStateMachine(), patientClient);
    }

    @Test
    void createChecksPatientAndStartsInDroitsAVerifier() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(repository.save(any(Rendezvous.class))).thenAnswer(inv -> inv.getArgument(0));

        RendezvousService.CreationResult result =
                service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID));

        verify(patientClient).assertPatientExists(1L);
        assertThat(result.created()).isTrue();
        assertThat(result.rendezvous().getStatut()).isEqualTo(RendezvousStatus.DROITS_A_VERIFIER);
    }

    @Test
    void createIsIdempotentOnRequestId() {
        Rendezvous existing = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        RendezvousService.CreationResult result =
                service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID));

        assertThat(result.created()).isFalse();
        assertThat(result.rendezvous()).isSameAs(existing);
        verifyNoInteractions(patientClient);
        verify(repository, never()).save(any());
    }

    @Test
    void createFailsWhenPatientUnknown() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        doThrow(new PatientNotFoundException(1L)).when(patientClient).assertPatientExists(1L);

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(PatientNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void changeStatutUsesStateMachine() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.DROITS_A_VERIFIER, REQUEST_ID);
        when(repository.findById(5L)).thenReturn(Optional.of(r));
        when(repository.save(r)).thenReturn(r);

        assertThat(service.changeStatut(5L, RendezvousStatus.PLANIFIE).getStatut())
                .isEqualTo(RendezvousStatus.PLANIFIE);
    }

    @Test
    void changeStatutRejectsInvalidTransition() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.ANNULE, REQUEST_ID);
        when(repository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.changeStatut(5L, RendezvousStatus.PLANIFIE))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(RendezvousNotFoundException.class);
    }
}
