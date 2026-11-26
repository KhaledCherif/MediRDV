package com.medirdv.rdvcore.dto;

import com.medirdv.praticien.grpc.Creneau;

/** A free slot received from praticien-service over gRPC. Dates are ISO-8601 strings, as in the proto. */
public record CreneauDto(long id, long praticienId, String praticienNom, String specialite, String debut, String fin) {

    public static CreneauDto from(Creneau c) {
        return new CreneauDto(c.getId(), c.getPraticienId(), c.getPraticienNom(), c.getSpecialite(),
                c.getDebut(), c.getFin());
    }
}
