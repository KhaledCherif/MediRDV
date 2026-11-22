package com.medirdv.rdvcore.dto;

/**
 * Patient as returned by patient-service (GET /api/v1/patients/{id}).
 * Only the fields rdv-core needs; unknown fields are ignored.
 */
public record PatientDto(Long id, String nom, String contact, String numeroAssure) {
}
