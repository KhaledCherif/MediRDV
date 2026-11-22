package com.medirdv.rdvcore.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medirdv.rdvcore.clients.PatientClient;
import com.medirdv.rdvcore.dto.CreateRendezvousRequest;
import com.medirdv.rdvcore.entities.Rendezvous;
import com.medirdv.rdvcore.enums.RendezvousStatus;
import com.medirdv.rdvcore.exceptions.RendezvousNotFoundException;
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

    public RendezvousService(RendezvousRepository repository,
                             RendezvousStateMachine stateMachine,
                             PatientClient patientClient) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.patientClient = patientClient;
    }

    /** Result of a create call: the rendezvous and whether it was newly created. */
    public record CreationResult(Rendezvous rendezvous, boolean created) {
    }

    /**
     * Creates a rendezvous in DROITS_A_VERIFIER (insurance rights still to check).
     * Idempotent on requestId: replaying the same requestId returns the existing rendezvous.
     */
    @Transactional
    public CreationResult create(CreateRendezvousRequest request) {
        Optional<Rendezvous> existing = repository.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return new CreationResult(existing.get(), false);
        }

        patientClient.assertPatientExists(request.patientId());

        Rendezvous rendezvous = new Rendezvous(
                request.patientId(),
                request.creneauId(),
                RendezvousStatus.DROITS_A_VERIFIER,
                request.requestId());
        return new CreationResult(repository.save(rendezvous), true);
    }

    @Transactional(readOnly = true)
    public Rendezvous findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RendezvousNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Rendezvous> findAll(Long patientId) {
        return patientId == null ? repository.findAll() : repository.findByPatientId(patientId);
    }

    /** Applies a status change; invalid transitions throw IllegalStateException (mapped to 409). */
    @Transactional
    public Rendezvous changeStatut(Long id, RendezvousStatus newStatut) {
        Rendezvous rendezvous = findById(id);
        stateMachine.transitionTo(rendezvous, newStatut);
        return repository.save(rendezvous);
    }
}
