package com.medirdv.assurancelegacy.repository;

import com.medirdv.assurancelegacy.model.Assurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour l'entite Assurance.

 */
@Repository
public interface AssuranceRepository extends JpaRepository<Assurance, Long> {

    Optional<Assurance> findByNumeroAssure(String numeroAssure);
}
