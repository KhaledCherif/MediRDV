package com.medirdv.rdvcore.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medirdv.rdvcore.entities.Rendezvous;

public interface RendezvousRepository extends JpaRepository<Rendezvous, Long> {
    boolean existsByRequestId(UUID requestId);

    Optional<Rendezvous> findByRequestId(UUID requestId);

    List<Rendezvous> findByPatientId(Long patientId);
}
