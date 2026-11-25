package com.medirdv.rdvcore.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.ws.test.client.RequestMatchers.payload;
import static org.springframework.ws.test.client.ResponseCreators.withException;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;
import static org.springframework.ws.test.client.ResponseCreators.withServerOrReceiverFault;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.xml.transform.Source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import com.medirdv.rdvcore.config.AssuranceSoapConfig;
import com.medirdv.rdvcore.dto.DroitsAssure;
import com.medirdv.rdvcore.exceptions.AssuranceServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.AssureInconnuException;

/**
 * Tests the SOAP client with Spring-WS MockWebServiceServer, using the same WebServiceTemplate
 * configuration as production (JAXB marshaller + AssureInconnu fault resolver).
 */
class AssuranceSoapClientTest {

    private static final String NS = "http://medirdv.com/assurance-legacy";

    private MockWebServiceServer server;
    private AssuranceSoapClient client;

    @BeforeEach
    void setUp() throws Exception {
        Jaxb2Marshaller marshaller = new AssuranceSoapConfig().assuranceMarshaller();
        marshaller.afterPropertiesSet();
        WebServiceTemplate template = new AssuranceSoapConfig()
                .assuranceWebServiceTemplate(marshaller, "http://assurance-legacy:8080/ws", Duration.ofSeconds(3));
        server = MockWebServiceServer.createServer(template);
        client = new AssuranceSoapClient(template);
    }

    @Test
    void verifierDroitsReturnsCoverage() {
        Source expectedRequest = new StringSource(
                "<verifierDroitsRequest xmlns='" + NS + "'><numeroAssure>ASSURE-001</numeroAssure></verifierDroitsRequest>");
        Source response = new StringSource(
                "<verifierDroitsResponse xmlns='" + NS + "'><couverture>true</couverture><taux>70.00</taux></verifierDroitsResponse>");
        server.expect(payload(expectedRequest)).andRespond(withPayload(response));

        DroitsAssure droits = client.verifierDroits("ASSURE-001");

        assertThat(droits.couverture()).isTrue();
        assertThat(droits.taux()).isEqualByComparingTo(new BigDecimal("70.00"));
        server.verify();
    }

    @Test
    void verifierDroitsAssureInconnuFaultIsIntercepted() {
        // Exactly what assurance-legacy sends: @SoapFault(faultCode = SERVER) + exception message
        server.expect(payload(new StringSource(
                        "<verifierDroitsRequest xmlns='" + NS + "'><numeroAssure>X-999</numeroAssure></verifierDroitsRequest>")))
                .andRespond(withServerOrReceiverFault("Assure inconnu : X-999", Locale.FRENCH));

        assertThatThrownBy(() -> client.verifierDroits("X-999"))
                .isInstanceOf(AssureInconnuException.class)
                .hasMessage("Assure inconnu : X-999");
    }

    @Test
    void otherSoapFaultIsATechnicalFailureNotAssureInconnu() {
        server.expect(payload(new StringSource(
                        "<verifierDroitsRequest xmlns='" + NS + "'><numeroAssure>ASSURE-001</numeroAssure></verifierDroitsRequest>")))
                .andRespond(withServerOrReceiverFault("Database connection lost", Locale.ENGLISH));

        assertThatThrownBy(() -> client.verifierDroits("ASSURE-001"))
                .isInstanceOf(AssuranceServiceUnavailableException.class)
                .isNotInstanceOf(AssureInconnuException.class);
    }

    @Test
    void networkErrorIsUnavailable() {
        server.expect(payload(new StringSource(
                        "<verifierDroitsRequest xmlns='" + NS + "'><numeroAssure>ASSURE-001</numeroAssure></verifierDroitsRequest>")))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.verifierDroits("ASSURE-001"))
                .isInstanceOf(AssuranceServiceUnavailableException.class);
    }

    @Test
    void declarerConsultationReturnsAccuse() {
        Source expectedRequest = new StringSource(
                "<declarerConsultationRequest xmlns='" + NS + "'>"
                        + "<numeroAssure>ASSURE-001</numeroAssure>"
                        + "<date>2026-12-01T09:00:00</date>"
                        + "<praticien>Dr Test</praticien>"
                        + "</declarerConsultationRequest>");
        Source response = new StringSource(
                "<declarerConsultationResponse xmlns='" + NS + "'><accuse>ACCUSE-ASSURE-001</accuse></declarerConsultationResponse>");
        server.expect(payload(expectedRequest)).andRespond(withPayload(response));

        String accuse = client.declarerConsultation("ASSURE-001", LocalDateTime.of(2026, 12, 1, 9, 0), "Dr Test");

        assertThat(accuse).isEqualTo("ACCUSE-ASSURE-001");
        server.verify();
    }

    @Test
    void declarerConsultationAssureInconnuFaultIsIntercepted() {
        server.expect(payload(new StringSource(
                        "<declarerConsultationRequest xmlns='" + NS + "'><numeroAssure>X-999</numeroAssure>"
                                + "<date>2026-12-01T09:00:00</date><praticien>Dr Test</praticien></declarerConsultationRequest>")))
                .andRespond(withServerOrReceiverFault("Assure inconnu : X-999", Locale.FRENCH));

        assertThatThrownBy(() -> client.declarerConsultation("X-999", LocalDateTime.of(2026, 12, 1, 9, 0), "Dr Test"))
                .isInstanceOf(AssureInconnuException.class);
    }
}
