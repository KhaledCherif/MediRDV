package com.medirdv.rdvcore.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRendezvousRequest(
        @NotNull @Positive
        @Schema(description = "Identifiant du patient (patient-service)", example = "1")
        Long patientId,

        @NotNull @Positive
        @Schema(description = "Identifiant du créneau (praticien-service)", example = "10")
        Long creneauId,

        @NotNull
        @Schema(description = "Clé d'idempotence fournie par le client (UUID)",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID requestId) {
}
