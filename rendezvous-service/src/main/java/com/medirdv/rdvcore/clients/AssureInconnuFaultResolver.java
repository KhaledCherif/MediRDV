package com.medirdv.rdvcore.clients;

import java.util.Locale;

import javax.xml.namespace.QName;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;

import com.medirdv.rdvcore.exceptions.AssureInconnuException;

/**
 * Intercepts SOAP faults returned by assurance-legacy.
 *
 * The AssureInconnu business fault becomes AssureInconnuException (HTTP 422).
 * Any other fault stays a SoapFaultClientException, which AssuranceSoapClient treats
 * as a technical failure, distinct from the business fault.
 *
 * assurance-legacy currently raises it with @SoapFault(faultCode = SERVER) and the message
 * "Assure inconnu : <numero>", so we match the fault string. A custom fault code
 * whose local part is AssureInconnu is accepted too, in case the server switches to FaultCode.CUSTOM.
 */
public class AssureInconnuFaultResolver implements FaultMessageResolver {

    static final String FAULT_NAME = "AssureInconnu";

    @Override
    public void resolveFault(WebServiceMessage message) {
        SoapMessage soapMessage = (SoapMessage) message;
        SoapFault fault = soapMessage.getSoapBody().getFault();
        if (fault != null && isAssureInconnu(fault)) {
            throw new AssureInconnuException(fault.getFaultStringOrReason());
        }
        throw new SoapFaultClientException(soapMessage);
    }

    static boolean isAssureInconnu(SoapFault fault) {
        QName code = fault.getFaultCode();
        if (code != null && FAULT_NAME.equalsIgnoreCase(code.getLocalPart())) {
            return true;
        }
        String reason = fault.getFaultStringOrReason();
        if (reason == null) {
            return false;
        }
        String normalized = reason.toLowerCase(Locale.ROOT).replace('é', 'e');
        return normalized.contains("assure inconnu") || normalized.contains("assureinconnu");
    }
}
