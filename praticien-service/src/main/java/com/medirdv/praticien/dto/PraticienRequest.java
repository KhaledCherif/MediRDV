package com.medirdv.praticien.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PraticienRequest(
        @NotBlank @Size(max = 120) String nom,
        @NotBlank @Size(max = 80) String specialite) {
}
