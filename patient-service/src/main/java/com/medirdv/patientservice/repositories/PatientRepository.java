package com.medirdv.patientservice.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medirdv.patientservice.entities.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

	Optional<Patient> findByNumeroAssure(String numeroAssure);

	boolean existsByNumeroAssure(String numeroAssure);

	boolean existsByNumeroAssureAndIdNot(String numeroAssure, Long id);
}
