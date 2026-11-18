package com.medirdv.patientservice.exceptions;

public class DuplicateNumeroAssureException extends RuntimeException {

	public DuplicateNumeroAssureException(String numeroAssure) {
		super("A patient with numeroAssure " + numeroAssure + " already exists");
	}

	public DuplicateNumeroAssureException(String numeroAssure, Throwable cause) {
		super("A patient with numeroAssure " + numeroAssure + " already exists", cause);
	}
}
