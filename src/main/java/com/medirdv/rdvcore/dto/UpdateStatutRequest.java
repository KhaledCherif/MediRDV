package com.medirdv.rdvcore.dto;

import com.medirdv.rdvcore.enums.RendezvousStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateStatutRequest(
        @NotNull
        @Schema(description = "Nouveau statut (transition validée par la machine à états)", example = "ANNULE")
        RendezvousStatus statut) {
}
