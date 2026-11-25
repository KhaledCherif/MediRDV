package com.medirdv.rdvcore.dto;

import com.medirdv.rdvcore.enums.RendezvousStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateStatutRequest(
        @NotNull
        @Schema(description = "Action publique : ANNULE ou CLOTURE", example = "ANNULE")
        RendezvousStatus statut) {
}
