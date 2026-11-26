package com.medirdv.rdvcore.services;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.medirdv.praticien.grpc.ReserverCreneauResponse;
import com.medirdv.rdvcore.clients.AssuranceSoapClient;
import com.medirdv.rdvcore.clients.PatientClient;
import com.medirdv.rdvcore.clients.PraticienGrpcClient;
import com.medirdv.rdvcore.clients.PraticienMetadataClient;
import com.medirdv.rdvcore.clients.PraticienMetadataClient.ConsultationMetadata;
import com.medirdv.rdvcore.dto.CreateRendezvousRequest;
import com.medirdv.rdvcore.dto.CreneauDto;
import com.medirdv.rdvcore.dto.CreneauxDisponibles;
import com.medirdv.rdvcore.dto.DroitsAssure;
import com.medirdv.rdvcore.dto.PatientDto;
import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;
import com.medirdv.rdvcore.exceptions.RendezvousNotFoundException;
import com.medirdv.rdvcore.exceptions.DroitsNonCouvertsException;
import com.medirdv.rdvcore.exceptions.PatientServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.RendezvousPersistenceException;
import com.medirdv.rdvcore.exceptions.AssuranceServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.AssureInconnuException;
import com.medirdv.rdvcore.exceptions.PraticienServiceUnavailableException;
import com.medirdv.rdvcore.repositories.RendezvousRepository;

/**
 * Application service behind /api/v1/rendezvous (#28).
 * Reuses the Rendezvous entity, repository and state machine from #26/#27
 * and the patient-service client from #24.
 */
@Service
public class RendezvousService {

    private final RendezvousRepository repository;
    private final RendezvousStateMachine stateMachine;
    private final PatientClient patientClient;
    private final PraticienGrpcClient praticienGrpcClient;
    private final AssuranceSoapClient assuranceSoapClient;
    private final PraticienMetadataClient praticienMetadataClient;
    private final TransactionTemplate writeTransaction;

    public RendezvousService(RendezvousRepository repository,
                             RendezvousStateMachine stateMachine,
                             PatientClient patientClient,
                             PraticienGrpcClient praticienGrpcClient,
                             AssuranceSoapClient assuranceSoapClient,
                             PraticienMetadataClient praticienMetadataClient,
                             PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.patientClient = patientClient;
        this.praticienGrpcClient = praticienGrpcClient;
        this.assuranceSoapClient = assuranceSoapClient;
        this.praticienMetadataClient = praticienMetadataClient;
        this.writeTransaction = new TransactionTemplate(transactionManager);
    }

    /** Result of a create call: the rendezvous and whether it was newly created. */
    public record CreationResult(Rendezvous rendezvous, boolean created) {
    }

    /** SOAP and gRPC run outside the short database transaction. */
    public CreationResult create(CreateRendezvousRequest request) {
        Optional<Rendezvous> existing = repository.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return replay(request, existing.get());
        }

        PatientDto patient = patientClient.getPatient(request.patientId());
        if (patient == null || !request.patientId().equals(patient.id())
                || patient.numeroAssure() == null || patient.numeroAssure().isBlank()) {
            throw new PatientServiceUnavailableException("patient-service returned no insurance number", null);
        }
        DroitsAssure droits = assuranceSoapClient.verifierDroits(patient.numeroAssure());
        if (droits == null) {
            throw new AssuranceServiceUnavailableException("assurance-legacy returned no rights result", null);
        }
        if (!droits.couverture()) {
            throw new DroitsNonCouvertsException("Patient has no insurance coverage");
        }

        ReserverCreneauResponse reservation = praticienGrpcClient.reserverCreneau(
                request.requestId().toString(), request.creneauId());
        if (reservation.getDejaTraite()) {
            return repository.findByRequestId(request.requestId())
                    .map(rendezvous -> replay(request, rendezvous))
                    .orElseThrow(() -> new IllegalStateException(
                            "Reservation request was processed remotely, but no rendezvous is recorded; reconcile before retrying"));
        }

        Rendezvous rendezvous = new Rendezvous(
                request.patientId(),
                request.creneauId(),
                RendezvousStatus.PLANIFIE,
                request.requestId());
        rendezvous.setPraticienId(reservation.getPraticienId());
        AtomicBoolean flushed = new AtomicBoolean(false);
        try {
            Rendezvous saved = writeTransaction.execute(status -> {
                Rendezvous result = repository.saveAndFlush(rendezvous);
                flushed.set(true);
                return result;
            });
            return new CreationResult(saved, true);
        } catch (RuntimeException persistenceFailure) {
            // A failed commit can still have reached PostgreSQL. Check before releasing remotely.
            Optional<Rendezvous> committed;
            try {
                committed = repository.findByRequestId(request.requestId());
            } catch (RuntimeException lookupFailure) {
                persistenceFailure.addSuppressed(lookupFailure);
                throw new RendezvousPersistenceException(
                        "Database outcome is unknown; slot was not released automatically", persistenceFailure);
            }
            if (committed.isPresent()) {
                return replay(request, committed.get());
            }
            if (flushed.get() && !(persistenceFailure instanceof UnexpectedRollbackException)
                    && !(persistenceFailure instanceof DataIntegrityViolationException)) {
                throw new RendezvousPersistenceException(
                        "Commit outcome is uncertain; slot was not released automatically", persistenceFailure);
            }
            try {
                praticienGrpcClient.libererCreneau(releaseRequestId(request.requestId()), request.creneauId());
            } catch (RuntimeException releaseFailure) {
                persistenceFailure.addSuppressed(releaseFailure);
                throw new RendezvousPersistenceException(
                        "Database failed and slot release failed; manual reconciliation is required", persistenceFailure);
            }
            throw new RendezvousPersistenceException("Database failed; reserved slot was released", persistenceFailure);
        }
    }

    private CreationResult replay(CreateRendezvousRequest request, Rendezvous rendezvous) {
        if (!Objects.equals(rendezvous.getPatientId(), request.patientId())
                || !Objects.equals(rendezvous.getCreneauId(), request.creneauId())) {
            throw new IllegalStateException("requestId already belongs to a different rendezvous");
        }
        return new CreationResult(rendezvous, false);
    }

    static String releaseRequestId(UUID reservationRequestId) {
        return UUID.nameUUIDFromBytes(("release:" + reservationRequestId)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Transactional(readOnly = true)
    public Rendezvous findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RendezvousNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Rendezvous> findAll(Long patientId) {
        return patientId == null ? repository.findAll() : repository.findByPatientId(patientId);
    }

    /**
     * Slot ids to exclude for this patient. Planned and pending-rights appointments
     * count as active; cancelled and closed appointments do not.
     */
    @Transactional(readOnly = true)
    public Set<Long> activeSlotIdsForPatient(Long patientId) {
        return repository.findByPatientId(patientId).stream()
                .filter(rendezvous -> rendezvous.getStatut() == RendezvousStatus.PLANIFIE
                        || rendezvous.getStatut() == RendezvousStatus.DROITS_A_VERIFIER)
                .map(Rendezvous::getCreneauId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Keeps the slot objects and ordering supplied by the streaming client. */
    @Transactional(readOnly = true)
    public <T> List<T> filterAvailableSlotsForPatient(
            Long patientId, List<T> availableSlots, ToLongFunction<T> slotId) {
        Set<Long> bookedSlotIds = activeSlotIdsForPatient(patientId);
        return availableSlots.stream()
                .filter(slot -> !bookedSlotIds.contains(slotId.applyAsLong(slot)))
                .toList();
    }

    public CreneauxDisponibles availableSlotsForPatient(
            Long patientId, String specialite, LocalDateTime du, LocalDateTime au) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("patientId must be positive");
        }
        if (du != null && au != null && !du.isBefore(au)) {
            throw new IllegalArgumentException("du must be before au");
        }
        CreneauxDisponibles streamed = praticienGrpcClient.listerCreneauxDisponibles(
                specialite, du == null ? null : du.toString(), au == null ? null : au.toString());
        List<CreneauDto> filtered = filterAvailableSlotsForPatient(
                patientId, streamed.creneaux(), CreneauDto::id);
        return new CreneauxDisponibles(filtered, streamed.partiel());
    }

    /** Only public cancellation and closure perform status changes. */
    public Rendezvous changeStatut(Long id, RendezvousStatus newStatut) {
        if (newStatut == RendezvousStatus.ANNULE) {
            return cancel(id);
        }
        if (newStatut == RendezvousStatus.CLOTURE) {
            return close(id);
        }
        throw new IllegalStateException("Direct transition to " + newStatut + " is not supported");
    }

    private Rendezvous cancel(Long id) {
        Rendezvous rendezvous = writeTransaction.execute(status -> {
            Rendezvous current = locked(id);
            if (current.getStatut() == RendezvousStatus.ANNULE) {
                return current;
            }
            stateMachine.transitionTo(current, RendezvousStatus.ANNULE);
            // Legacy rows have no practitioner id and no confirmed remote reservation.
            current.setPendingLiberation(current.getPraticienId() != null);
            return repository.saveAndFlush(current);
        });
        if (!rendezvous.isPendingLiberation()) {
            return rendezvous;
        }
        try {
            praticienGrpcClient.libererCreneau(
                    releaseRequestId(rendezvous.getRequestId()), rendezvous.getCreneauId());
        } catch (RuntimeException e) {
            throw new PraticienServiceUnavailableException(
                    "Cancellation is recorded, but slot release is pending", e);
        }
        try {
            return writeTransaction.execute(status -> {
                Rendezvous current = locked(id);
                current.setPendingLiberation(false);
                return repository.saveAndFlush(current);
            });
        } catch (RuntimeException e) {
            throw new RendezvousPersistenceException(
                    "Slot was released, but the pending flag could not be cleared", e);
        }
    }

    private Rendezvous close(Long id) {
        Rendezvous before = findById(id);
        if (before.getStatut() == RendezvousStatus.CLOTURE) {
            return before; // SOAP has no idempotency key: never redeclare on replay.
        }
        if (before.getStatut() != RendezvousStatus.PLANIFIE) {
            throw new IllegalStateException("Invalid transition from " + before.getStatut() + " to CLOTURE");
        }
        if (before.getPraticienId() == null) {
            throw new IllegalStateException("Practitioner metadata is missing for this legacy rendezvous");
        }
        ConsultationMetadata metadata = praticienMetadataClient.consultation(
                before.getPraticienId(), before.getCreneauId());
        PatientDto patient = patientClient.getPatient(before.getPatientId());
        if (patient == null || !before.getPatientId().equals(patient.id())
                || patient.numeroAssure() == null || patient.numeroAssure().isBlank()) {
            throw new PatientServiceUnavailableException("patient-service returned no insurance number", null);
        }

        TransitionResult prepared = writeTransaction.execute(status -> {
            Rendezvous current = locked(id);
            if (current.getStatut() == RendezvousStatus.CLOTURE) {
                return new TransitionResult(current, false);
            }
            stateMachine.transitionTo(current, RendezvousStatus.CLOTURE);
            current.setPendingDeclaration(true);
            return new TransitionResult(repository.saveAndFlush(current), true);
        });
        if (!prepared.changed()) {
            return prepared.rendezvous();
        }
        try {
            String accuse = assuranceSoapClient.declarerConsultation(
                    patient.numeroAssure(), metadata.date(), metadata.praticien());
            if (accuse == null || accuse.isBlank()) {
                return prepared.rendezvous();
            }
        } catch (AssuranceServiceUnavailableException e) {
            return prepared.rendezvous(); // visible as pending; recovery is a later milestone.
        } catch (AssureInconnuException e) {
            throw e; // business fault remains distinct from a technical outage.
        }
        try {
            return writeTransaction.execute(status -> {
                Rendezvous current = locked(id);
                current.setPendingDeclaration(false);
                return repository.saveAndFlush(current);
            });
        } catch (RuntimeException e) {
            throw new RendezvousPersistenceException(
                    "Consultation was declared, but the pending flag could not be cleared", e);
        }
    }

    private Rendezvous locked(Long id) {
        return repository.findByIdForUpdate(id).orElseThrow(() -> new RendezvousNotFoundException(id));
    }

    private record TransitionResult(Rendezvous rendezvous, boolean changed) {
    }
}
