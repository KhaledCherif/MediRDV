package com.medirdv.rdvcore.dto;

import java.util.List;

/**
 * Result of the ListerCreneauxDisponibles stream.
 * partiel = true when the 5s streaming deadline expired before the end of the stream.
 */
public record CreneauxDisponibles(List<CreneauDto> creneaux, boolean partiel) {
}
