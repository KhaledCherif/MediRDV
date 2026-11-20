package com.medirdv.praticien.dto;

import com.medirdv.praticien.domain.Praticien;

public record PraticienResponse(Long id, String nom, String specialite) {
    public static PraticienResponse from(Praticien p) {
        return new PraticienResponse(p.getId(), p.getNom(), p.getSpecialite());
    }
}
