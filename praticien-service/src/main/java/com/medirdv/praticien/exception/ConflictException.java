package com.medirdv.praticien.exception;

/** Règle métier violée (créneau déjà réservé, chevauchement...). Mappée en HTTP 409 / gRPC FAILED_PRECONDITION. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
