package com.medirdv.assurancelegacy.service;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Exception levee lorsqu'un assure est introuvable.
 * Transformee automatiquement en SOAP fault cote serveur.
 */
@SoapFault(faultCode = FaultCode.CLIENT, faultStringOrReason = "Assure inconnu")
public class AssureInconnuException extends RuntimeException {

    public AssureInconnuException(String message) {
        super(message);
    }
}
