package com.medirdv.rdvcore.clients;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import com.medirdv.rdvcore.dto.DroitsAssure;
import com.medirdv.rdvcore.exceptions.AssuranceServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.AssureInconnuException;
import com.medirdv.rdvcore.soap.gen.DeclarerConsultationRequest;
import com.medirdv.rdvcore.soap.gen.DeclarerConsultationResponse;
import com.medirdv.rdvcore.soap.gen.ObjectFactory;
import com.medirdv.rdvcore.soap.gen.VerifierDroitsRequest;
import com.medirdv.rdvcore.soap.gen.VerifierDroitsResponse;

/**
 * SOAP client toward assurance-legacy (Suivi 2, Membre 5), built on WebServiceTemplate + JAXB.
 *
 * Errors are split in two families, as the guide requires:
 *  - AssureInconnuException: business fault, the insured number does not exist (HTTP 422);
 *  - AssuranceServiceUnavailableException: timeout (3s), connection refused or any other fault (HTTP 503),
 *    so the orchestration can fall back to DROITS_A_VERIFIER.
 */
@Component
public class AssuranceSoapClient {

    /** xs:dateTime needs the seconds; LocalDateTime.toString() drops them when they are 0. */
    private static final DateTimeFormatter XS_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final WebServiceTemplate template;
    private final ObjectFactory factory = new ObjectFactory();

    public AssuranceSoapClient(WebServiceTemplate assuranceWebServiceTemplate) {
        this.template = assuranceWebServiceTemplate;
    }

    /** VerifierDroits(numeroAssure). */
    public DroitsAssure verifierDroits(String numeroAssure) {
        VerifierDroitsRequest request = factory.createVerifierDroitsRequest();
        request.setNumeroAssure(numeroAssure);

        VerifierDroitsResponse response = call(request, VerifierDroitsResponse.class, "VerifierDroits");
        return new DroitsAssure(response.isCouverture(), response.getTaux());
    }

    /** DeclarerConsultation(numeroAssure, date, praticien). Returns the acknowledgement (accuse). */
    public String declarerConsultation(String numeroAssure, LocalDateTime date, String praticien) {
        DeclarerConsultationRequest request = factory.createDeclarerConsultationRequest();
        request.setNumeroAssure(numeroAssure);
        request.setDate(toXml(date));
        request.setPraticien(praticien);

        DeclarerConsultationResponse response =
                call(request, DeclarerConsultationResponse.class, "DeclarerConsultation");
        return response.getAccuse();
    }

    private <T> T call(Object request, Class<T> responseType, String operation) {
        try {
            return responseType.cast(template.marshalSendAndReceive(request));
        } catch (AssureInconnuException e) {
            throw e; // business fault, intercepted by AssureInconnuFaultResolver
        } catch (WebServiceIOException e) {
            throw new AssuranceServiceUnavailableException(
                    "assurance-legacy unreachable or slower than the timeout on " + operation, e);
        } catch (SoapFaultClientException e) {
            throw new AssuranceServiceUnavailableException(
                    "assurance-legacy returned a technical fault on " + operation + ": " + e.getFaultStringOrReason(), e);
        }
    }

    private static XMLGregorianCalendar toXml(LocalDateTime date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date.format(XS_DATE_TIME));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("No XML DatatypeFactory available", e);
        }
    }
}
