package com.medirdv.rdvcore.exceptions;

/** SOAP business fault AssureInconnu from assurance-legacy. Mapped to HTTP 422. */
public class AssureInconnuException extends RuntimeException {

    public AssureInconnuException(String message) {
        super(message != null ? message : "Assure inconnu");
    }
}
