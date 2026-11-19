package com.medirdv.patientservice.services;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medirdv.patientservice.entities.Patient;
import com.medirdv.patientservice.exceptions.PatientNotFoundException;
import com.medirdv.patientservice.exceptions.DuplicateNumeroAssureException;
import com.medirdv.patientservice.repositories.PatientRepository;

@Service
@Transactional
public class PatientService {

	private final PatientRepository patientRepository;

	public PatientService(PatientRepository patientRepository) {
		this.patientRepository = patientRepository;
	}

	@Transactional(readOnly = true)
	public List<Patient> findAll() {
		return patientRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Patient findById(Long id) {
		return patientRepository.findById(id)
				.orElseThrow(() -> new PatientNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Patient findByNumeroAssure(String numeroAssure) {
		return patientRepository.findByNumeroAssure(numeroAssure)
				.orElseThrow(() -> new PatientNotFoundException(numeroAssure));
	}

	public Patient create(Patient patient) {
		if (patientRepository.existsByNumeroAssure(patient.getNumeroAssure())) {
			throw new DuplicateNumeroAssureException(patient.getNumeroAssure());
		}

		Patient newPatient = new Patient();
		copyFields(patient, newPatient);
		try {
			return patientRepository.saveAndFlush(newPatient);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateNumeroAssureException(patient.getNumeroAssure(), exception);
		}
	}

	public Patient update(Long id, Patient patient) {
		Patient existingPatient = findById(id);
		if (patientRepository.existsByNumeroAssureAndIdNot(patient.getNumeroAssure(), id)) {
			throw new DuplicateNumeroAssureException(patient.getNumeroAssure());
		}

		copyFields(patient, existingPatient);
		try {
			return patientRepository.saveAndFlush(existingPatient);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateNumeroAssureException(patient.getNumeroAssure(), exception);
		}
	}

	public void delete(Long id) {
		patientRepository.delete(findById(id));
	}

	private void copyFields(Patient source, Patient target) {
		target.setNom(source.getNom());
		target.setContact(source.getContact());
		target.setNumeroAssure(source.getNumeroAssure());
	}
}
