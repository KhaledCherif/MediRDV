package com.medirdv.praticien.dto;

import java.time.LocalDateTime;

public record CreneauDisponible(Long id, Long praticienId, String praticienNom, String specialite,
                                LocalDateTime debut, LocalDateTime fin) {
}
