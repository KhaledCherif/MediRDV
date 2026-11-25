package com.medirdv.rdvcore.dto;

import java.math.BigDecimal;

/** Answer of the SOAP VerifierDroits operation. */
public record DroitsAssure(boolean couverture, BigDecimal taux) {
}
