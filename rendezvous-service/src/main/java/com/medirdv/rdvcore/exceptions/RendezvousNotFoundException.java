package com.medirdv.rdvcore.exceptions;

public class RendezvousNotFoundException extends RuntimeException {

    public RendezvousNotFoundException(Long id) {
        super("Rendezvous not found with id: " + id);
    }
}
