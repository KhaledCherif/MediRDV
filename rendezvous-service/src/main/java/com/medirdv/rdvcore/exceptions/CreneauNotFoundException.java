package com.medirdv.rdvcore.exceptions;

/** gRPC NOT_FOUND from praticien-service. Mapped to HTTP 404. */
public class CreneauNotFoundException extends RuntimeException {

    public CreneauNotFoundException(Long creneauId, String detail) {
        super(detail != null ? detail : "Creneau not found: " + creneauId);
    }
}
