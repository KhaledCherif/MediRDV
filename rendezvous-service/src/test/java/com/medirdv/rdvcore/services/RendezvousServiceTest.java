package com.medirdv.rdvcore.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;

import java.math.BigDecimal;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medirdv.praticien.grpc.ReserverCreneauResponse;
import com.medirdv.rdvcore.clients.AssuranceSoapClient;
import com.medirdv.rdvcore.clients.PatientClient;
import com.medirdv.rdvcore.clients.PraticienMetadataClient;
import com.medirdv.rdvcore.clients.PraticienMetadataClient.ConsultationMetadata;
import com.medirdv.rdvcore.clients.PraticienGrpcClient;
import com.medirdv.rdvcore.dto.CreateRendezvousRequest;
import com.medirdv.rdvcore.dto.CreneauDto;
import com.medirdv.rdvcore.dto.CreneauxDisponibles;
import com.medirdv.rdvcore.dto.DroitsAssure;
import com.medirdv.rdvcore.dto.PatientDto;
import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;
import com.medirdv.rdvcore.exceptions.PatientNotFoundException;
import com.medirdv.rdvcore.exceptions.AssureInconnuException;
import com.medirdv.rdvcore.exceptions.AssuranceServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.CreneauDejaReserveException;
import com.medirdv.rdvcore.exceptions.DroitsNonCouvertsException;
import com.medirdv.rdvcore.exceptions.RendezvousPersistenceException;
import com.medirdv.rdvcore.exceptions.PraticienServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.RendezvousNotFoundException;
import com.medirdv.rdvcore.repositories.RendezvousRepository;

@ExtendWith(MockitoExtension.class)
class RendezvousServiceTest {

    private static final UUID REQUEST_ID = UUID.randomUUID();

    @Mock
    private RendezvousRepository repository;

    @Mock
    private PatientClient patientClient;

    @Mock
    private PraticienGrpcClient praticienGrpcClient;

    @Mock
    private AssuranceSoapClient assuranceSoapClient;

    @Mock
    private PraticienMetadataClient praticienMetadataClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    private RendezvousService service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any()))
                .thenAnswer(inv -> new SimpleTransactionStatus());
        service = new RendezvousService(repository, new RendezvousStateMachine(), patientClient,
                praticienGrpcClient, assuranceSoapClient, praticienMetadataClient, transactionManager);
    }

    private void successfulRemoteReservation() {
        when(patientClient.getPatient(1L)).thenReturn(new PatientDto(1L, "A", "contact", "ASSURE-001"));
        when(assuranceSoapClient.verifierDroits("ASSURE-001"))
                .thenReturn(new DroitsAssure(true, new BigDecimal("0.80")));
        when(praticienGrpcClient.reserverCreneau(REQUEST_ID.toString(), 10L))
                .thenReturn(ReserverCreneauResponse.newBuilder()
                        .setCreneauId(10L).setPraticienId(7L).build());
    }

    @Test
    void createChecksRightsReservesSlotAndPersistsPlannedAppointment() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        successfulRemoteReservation();
        when(repository.saveAndFlush(any(Rendezvous.class))).thenAnswer(inv -> inv.getArgument(0));

        RendezvousService.CreationResult result =
                service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID));

        verify(patientClient).getPatient(1L);
        verify(assuranceSoapClient).verifierDroits("ASSURE-001");
        verify(praticienGrpcClient).reserverCreneau(REQUEST_ID.toString(), 10L);
        assertThat(result.created()).isTrue();
        assertThat(result.rendezvous().getStatut()).isEqualTo(RendezvousStatus.PLANIFIE);
        assertThat(result.rendezvous().getPraticienId()).isEqualTo(7L);
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
        verifyNoInteractions(assuranceSoapClient, praticienGrpcClient);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsRequestIdReusedForAnotherBooking() {
        Rendezvous existing = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 11L, REQUEST_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requestId");
        verifyNoInteractions(patientClient);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createFailsWhenPatientUnknown() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(patientClient.getPatient(1L)).thenThrow(new PatientNotFoundException(1L));

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(PatientNotFoundException.class);
        verifyNoInteractions(assuranceSoapClient, praticienGrpcClient);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void unknownInsuredStopsBeforeGrpcAndReturnsBusinessFault() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(patientClient.getPatient(1L)).thenReturn(new PatientDto(1L, "A", "contact", "UNKNOWN"));
        when(assuranceSoapClient.verifierDroits("UNKNOWN"))
                .thenThrow(new AssureInconnuException("Assure inconnu"));

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(AssureInconnuException.class);
        verifyNoInteractions(praticienGrpcClient);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void noCoverageRejectsBeforeGrpc() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(patientClient.getPatient(1L)).thenReturn(new PatientDto(1L, "A", "contact", "ASSURE-001"));
        when(assuranceSoapClient.verifierDroits("ASSURE-001"))
                .thenReturn(new DroitsAssure(false, BigDecimal.ZERO));

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(DroitsNonCouvertsException.class);
        verifyNoInteractions(praticienGrpcClient);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void soapOutageStopsBeforeGrpc() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(patientClient.getPatient(1L)).thenReturn(new PatientDto(1L, "A", "contact", "ASSURE-001"));
        when(assuranceSoapClient.verifierDroits("ASSURE-001"))
                .thenThrow(new AssuranceServiceUnavailableException("SOAP unavailable", null));

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(AssuranceServiceUnavailableException.class);
        verifyNoInteractions(praticienGrpcClient);
    }

    @Test
    void doubleBookingDoesNotWriteRendezvous() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        successfulRemoteReservation();
        when(praticienGrpcClient.reserverCreneau(REQUEST_ID.toString(), 10L))
                .thenThrow(new CreneauDejaReserveException(10L, "already booked"));

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(CreneauDejaReserveException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void previouslyProcessedGrpcRequestWithoutLocalRendezvousIsNotReportedAsSuccess() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        successfulRemoteReservation();
        when(praticienGrpcClient.reserverCreneau(REQUEST_ID.toString(), 10L))
                .thenReturn(ReserverCreneauResponse.newBuilder()
                        .setCreneauId(10L).setPraticienId(7L).setDejaTraite(true).build());

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reconcile");
        verify(repository, never()).saveAndFlush(any());
        verify(praticienGrpcClient, never()).libererCreneau(any(), any(Long.class));
    }

    @Test
    void concurrentCommittedReplayIsReturnedWithoutReleasingSlot() {
        Rendezvous committed = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        committed.setPraticienId(7L);
        when(repository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(committed));
        successfulRemoteReservation();
        when(repository.saveAndFlush(any(Rendezvous.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new DataAccessResourceFailureException("commit failed"))
                .when(transactionManager).commit(any());

        RendezvousService.CreationResult result =
                service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID));

        assertThat(result.created()).isFalse();
        assertThat(result.rendezvous()).isSameAs(committed);
        verify(praticienGrpcClient, never()).libererCreneau(any(), any(Long.class));
    }

    @Test
    void knownRollbackAtCommitReleasesWithAStableDistinctRequestId() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        successfulRemoteReservation();
        when(repository.saveAndFlush(any(Rendezvous.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new UnexpectedRollbackException("rolled back at commit"))
                .when(transactionManager).commit(any());

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(RendezvousPersistenceException.class);
        String releaseId = RendezvousService.releaseRequestId(REQUEST_ID);
        assertThat(releaseId).isNotEqualTo(REQUEST_ID.toString());
        assertThat(releaseId).isEqualTo(RendezvousService.releaseRequestId(REQUEST_ID));
        verify(praticienGrpcClient).libererCreneau(releaseId, 10L);
    }

    @Test
    void failedFlushReleasesReservedSlot() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        successfulRemoteReservation();
        when(repository.saveAndFlush(any(Rendezvous.class)))
                .thenThrow(new DataAccessResourceFailureException("write failed"));

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(RendezvousPersistenceException.class);
        verify(praticienGrpcClient).libererCreneau(RendezvousService.releaseRequestId(REQUEST_ID), 10L);
    }

    @Test
    void uncertainCommitWithEmptyLookupDoesNotReleaseSlot() {
        when(repository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        successfulRemoteReservation();
        when(repository.saveAndFlush(any(Rendezvous.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new DataAccessResourceFailureException("commit response lost"))
                .when(transactionManager).commit(any());

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(RendezvousPersistenceException.class)
                .hasMessageContaining("uncertain");
        verify(praticienGrpcClient, never()).libererCreneau(any(), any(Long.class));
    }

    @Test
    void uncertainCommitDoesNotReleaseSlot() {
        when(repository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.empty())
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        successfulRemoteReservation();
        when(repository.saveAndFlush(any(Rendezvous.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new DataAccessResourceFailureException("commit uncertain"))
                .when(transactionManager).commit(any());

        assertThatThrownBy(() -> service.create(new CreateRendezvousRequest(1L, 10L, REQUEST_ID)))
                .isInstanceOf(RendezvousPersistenceException.class)
                .hasMessageContaining("unknown");
        verify(praticienGrpcClient, never()).libererCreneau(any(), any(Long.class));
    }

    @Test
    void cancellationPersistsThenReleasesWithStableId() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        r.setPraticienId(7L);
        when(repository.findByIdForUpdate(5L)).thenReturn(Optional.of(r));
        when(repository.saveAndFlush(r)).thenReturn(r);

        assertThat(service.changeStatut(5L, RendezvousStatus.ANNULE).getStatut())
                .isEqualTo(RendezvousStatus.ANNULE);
        assertThat(r.isPendingLiberation()).isFalse();
        verify(praticienGrpcClient).libererCreneau(RendezvousService.releaseRequestId(REQUEST_ID), 10L);
    }

    @Test
    void changeStatutRejectsInvalidTransition() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.ANNULE, REQUEST_ID);
        when(repository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.changeStatut(5L, RendezvousStatus.CLOTURE))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void repeatedCancellationDoesNotReleaseAgain() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.ANNULE, REQUEST_ID);
        r.setPraticienId(7L);
        when(repository.findByIdForUpdate(5L)).thenReturn(Optional.of(r));

        assertThat(service.changeStatut(5L, RendezvousStatus.ANNULE)).isSameAs(r);
        verifyNoInteractions(praticienGrpcClient);
    }

    @Test
    void pendingCancellationRetriesTheSameReleaseOperation() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.ANNULE, REQUEST_ID);
        r.setPraticienId(7L);
        r.setPendingLiberation(true);
        when(repository.findByIdForUpdate(5L)).thenReturn(Optional.of(r));
        when(repository.saveAndFlush(r)).thenReturn(r);

        assertThat(service.changeStatut(5L, RendezvousStatus.ANNULE).isPendingLiberation()).isFalse();
        verify(praticienGrpcClient).libererCreneau(RendezvousService.releaseRequestId(REQUEST_ID), 10L);
    }

    @Test
    void releaseFailureLeavesCancellationPendingForSafeRetry() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        r.setPraticienId(7L);
        when(repository.findByIdForUpdate(5L)).thenReturn(Optional.of(r));
        when(repository.saveAndFlush(r)).thenReturn(r);
        doThrow(new PraticienServiceUnavailableException("down", null))
                .when(praticienGrpcClient).libererCreneau(any(), any(Long.class));

        assertThatThrownBy(() -> service.changeStatut(5L, RendezvousStatus.ANNULE))
                .isInstanceOf(PraticienServiceUnavailableException.class);
        assertThat(r.isPendingLiberation()).isTrue();
    }

    @Test
    void closureDeclaresRealConsultationAndClearsPendingFlag() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        r.setPraticienId(7L);
        LocalDateTime date = LocalDateTime.of(2026, 11, 26, 10, 0);
        when(repository.findById(5L)).thenReturn(Optional.of(r));
        when(repository.findByIdForUpdate(5L)).thenReturn(Optional.of(r));
        when(repository.saveAndFlush(r)).thenReturn(r);
        when(praticienMetadataClient.consultation(7L, 10L))
                .thenReturn(new ConsultationMetadata(date, "Dr A"));
        when(patientClient.getPatient(1L)).thenReturn(new PatientDto(1L, "A", "contact", "ASSURE-001"));
        when(assuranceSoapClient.declarerConsultation("ASSURE-001", date, "Dr A"))
                .thenReturn("ACCUSE-1");

        Rendezvous closed = service.changeStatut(5L, RendezvousStatus.CLOTURE);

        assertThat(closed.getStatut()).isEqualTo(RendezvousStatus.CLOTURE);
        assertThat(closed.isPendingDeclaration()).isFalse();
        verify(assuranceSoapClient).declarerConsultation("ASSURE-001", date, "Dr A");
    }

    @Test
    void failedDeclarationLeavesClosurePendingWithoutAutomaticRetry() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        r.setPraticienId(7L);
        LocalDateTime date = LocalDateTime.of(2026, 11, 26, 10, 0);
        when(repository.findById(5L)).thenReturn(Optional.of(r));
        when(repository.findByIdForUpdate(5L)).thenReturn(Optional.of(r));
        when(repository.saveAndFlush(r)).thenReturn(r);
        when(praticienMetadataClient.consultation(7L, 10L))
                .thenReturn(new ConsultationMetadata(date, "Dr A"));
        when(patientClient.getPatient(1L)).thenReturn(new PatientDto(1L, "A", "contact", "ASSURE-001"));
        when(assuranceSoapClient.declarerConsultation("ASSURE-001", date, "Dr A"))
                .thenThrow(new AssuranceServiceUnavailableException("down", null));

        assertThat(service.changeStatut(5L, RendezvousStatus.CLOTURE).isPendingDeclaration()).isTrue();
        assertThat(service.changeStatut(5L, RendezvousStatus.CLOTURE).isPendingDeclaration()).isTrue();
        verify(assuranceSoapClient).declarerConsultation("ASSURE-001", date, "Dr A");
    }

    @Test
    void legacyAppointmentWithoutPractitionerIdCannotBeClosed() {
        Rendezvous r = new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, REQUEST_ID);
        when(repository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.changeStatut(5L, RendezvousStatus.CLOTURE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("metadata");
        verifyNoInteractions(assuranceSoapClient, praticienMetadataClient);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(RendezvousNotFoundException.class);
    }

    @Test
    void activeSlotIdsIncludeOnlyCurrentBookingsForThePatient() {
        when(repository.findByPatientId(1L)).thenReturn(List.of(
                new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, UUID.randomUUID()),
                new Rendezvous(1L, 11L, RendezvousStatus.DROITS_A_VERIFIER, UUID.randomUUID()),
                new Rendezvous(1L, 12L, RendezvousStatus.ANNULE, UUID.randomUUID()),
                new Rendezvous(1L, 13L, RendezvousStatus.CLOTURE, UUID.randomUUID()),
                new Rendezvous(1L, 10L, RendezvousStatus.PLANIFIE, UUID.randomUUID())));

        assertThat(service.activeSlotIdsForPatient(1L)).isEqualTo(Set.of(10L, 11L));
        verify(repository).findByPatientId(1L);
    }

    @Test
    void activeSlotIdsAreEmptyWhenPatientHasNoBookings() {
        when(repository.findByPatientId(2L)).thenReturn(List.of());

        assertThat(service.activeSlotIdsForPatient(2L)).isEmpty();
    }

    @Test
    void filterAvailableSlotsPreservesUnbookedObjectsAndStreamOrder() {
        record Slot(long id, String practitioner) {}
        Slot first = new Slot(20L, "A");
        Slot booked = new Slot(21L, "B");
        Slot last = new Slot(22L, "C");
        when(repository.findByPatientId(1L)).thenReturn(List.of(
                new Rendezvous(1L, 21L, RendezvousStatus.PLANIFIE, UUID.randomUUID())));

        List<Slot> result = service.filterAvailableSlotsForPatient(
                1L, List.of(first, booked, last), Slot::id);

        assertThat(result).containsExactly(first, last);
    }

    @Test
    void availableSlotsFiltersBookingsAndPreservesPartialFlag() {
        CreneauDto first = new CreneauDto(20L, 2L, "Dr A", "cardio", "2026-11-26T10:00", "2026-11-26T10:30");
        CreneauDto booked = new CreneauDto(21L, 3L, "Dr B", "cardio", "2026-11-26T11:00", "2026-11-26T11:30");
        when(praticienGrpcClient.listerCreneauxDisponibles("cardio", null, null))
                .thenReturn(new CreneauxDisponibles(List.of(first, booked), true));
        when(repository.findByPatientId(1L)).thenReturn(List.of(
                new Rendezvous(1L, 21L, RendezvousStatus.PLANIFIE, UUID.randomUUID())));

        CreneauxDisponibles result = service.availableSlotsForPatient(1L, "cardio", null, null);

        assertThat(result.creneaux()).containsExactly(first);
        assertThat(result.partiel()).isTrue();
    }

    @Test
    void availableSlotsPreservesCompleteAndEmptyResults() {
        CreneauDto slot = new CreneauDto(20L, 2L, "Dr A", "cardio", "2026-11-26T10:00", "2026-11-26T10:30");
        when(praticienGrpcClient.listerCreneauxDisponibles(null, null, null))
                .thenReturn(new CreneauxDisponibles(List.of(slot), false))
                .thenReturn(new CreneauxDisponibles(List.of(), false));
        when(repository.findByPatientId(1L)).thenReturn(List.of());

        assertThat(service.availableSlotsForPatient(1L, null, null, null).creneaux()).containsExactly(slot);
        CreneauxDisponibles empty = service.availableSlotsForPatient(1L, null, null, null);
        assertThat(empty.creneaux()).isEmpty();
        assertThat(empty.partiel()).isFalse();
    }

    @Test
    void availableSlotsRejectsReversedDatesBeforeCallingGrpc() {
        assertThatThrownBy(() -> service.availableSlotsForPatient(1L, null,
                LocalDateTime.of(2026, 11, 27, 0, 0), LocalDateTime.of(2026, 11, 26, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(praticienGrpcClient);
    }
}
