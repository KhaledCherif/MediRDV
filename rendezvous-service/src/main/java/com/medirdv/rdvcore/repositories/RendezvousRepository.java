package com.medirdv.rdvcore.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.medirdv.rdvcore.entities.Rendezvous;

public interface RendezvousRepository extends JpaRepository<Rendezvous, Long> {
    boolean existsByRequestId(UUID requestId);

    Optional<Rendezvous> findByRequestId(UUID requestId);

    List<Rendezvous> findByPatientId(Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rendezvous r where r.id = :id")
    Optional<Rendezvous> findByIdForUpdate(@Param("id") Long id);
}
