package com.medirdv.rdvcore.exceptions;

/** assurance-legacy down, past the 3s timeout or returning a technical fault. Mapped to HTTP 503. */
public class AssuranceServiceUnavailableException extends RuntimeException {

    public AssuranceServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
