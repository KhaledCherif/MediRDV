package com.medirdv.rdvcore.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.medirdv.rdvcore.entities.Rendezvous;

import java.util.UUID;

public interface RendezvousRepository extends JpaRepository<Rendezvous, Long> {
    boolean existsByRequestId(UUID requestId);
}