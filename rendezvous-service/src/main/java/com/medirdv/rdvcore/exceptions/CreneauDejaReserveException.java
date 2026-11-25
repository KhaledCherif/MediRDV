package com.medirdv.rdvcore.exceptions;

/** gRPC FAILED_PRECONDITION from praticien-service (slot already booked). Mapped to HTTP 409. */
public class CreneauDejaReserveException extends RuntimeException {

    public CreneauDejaReserveException(Long creneauId, String detail) {
        super(detail != null ? detail : "Creneau already reserved: " + creneauId);
    }
}
