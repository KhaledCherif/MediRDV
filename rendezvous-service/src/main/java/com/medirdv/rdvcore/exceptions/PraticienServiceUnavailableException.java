package com.medirdv.rdvcore.exceptions;

/** praticien-service down, unreachable or past the gRPC deadline. Mapped to HTTP 503. */
public class PraticienServiceUnavailableException extends RuntimeException {

    public PraticienServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
