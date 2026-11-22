package com.medirdv.praticien.dto;

import com.medirdv.praticien.domain.Creneau;
import com.medirdv.praticien.domain.StatutCreneau;

import java.time.LocalDateTime;

public record CreneauResponse(Long id, Long praticienId, LocalDateTime debut, LocalDateTime fin,
                              StatutCreneau statut) {
    public static CreneauResponse from(Creneau c) {
        return new CreneauResponse(c.getId(), c.getPraticien().getId(), c.getDebut(), c.getFin(), c.getStatut());
    }
}
