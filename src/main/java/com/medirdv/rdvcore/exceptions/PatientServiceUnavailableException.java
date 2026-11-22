package com.medirdv.rdvcore.exceptions;

public class PatientServiceUnavailableException extends RuntimeException {

    public PatientServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
