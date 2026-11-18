package com.medirdv.patientservice.exceptions;

public class PatientNotFoundException extends RuntimeException {

	public PatientNotFoundException(Long id) {
		super("Patient with id " + id + " was not found");
	}

	public PatientNotFoundException(String numeroAssure) {
		super("Patient with numeroAssure " + numeroAssure + " was not found");
	}
}
