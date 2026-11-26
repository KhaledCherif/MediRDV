package com.medirdv.rdvcore.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import com.medirdv.rdvcore.clients.AssureInconnuFaultResolver;

/**
 * WebServiceTemplate toward assurance-legacy (Suivi 2).
 * URL comes from ASSURANCE_SOAP_URL (Docker DNS: http://assurance-legacy:8080/ws).
 * Connect and read timeouts are both 3s, so a SOAP call never waits more than about 3s for an answer.
 */
@Configuration
public class AssuranceSoapConfig {

    /** Package of the JAXB classes generated from contracts/assurance.xsd. */
    public static final String JAXB_PACKAGE = "com.medirdv.rdvcore.soap.gen";

    @Bean
    public Jaxb2Marshaller assuranceMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(JAXB_PACKAGE);
        return marshaller;
    }

    @Bean
    public WebServiceTemplate assuranceWebServiceTemplate(
            Jaxb2Marshaller assuranceMarshaller,
            @Value("${assurance-legacy.soap.url}") String url,
            @Value("${assurance-legacy.soap.timeout:3s}") Duration timeout) {

        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(timeout);
        sender.setReadTimeout(timeout);

        WebServiceTemplate template = new WebServiceTemplate(assuranceMarshaller);
        template.setDefaultUri(url);
        template.setMessageSender(sender);
        // Intercepts the AssureInconnu business fault before Spring-WS turns it into a generic exception
        template.setFaultMessageResolver(new AssureInconnuFaultResolver());
        return template;
    }
}
