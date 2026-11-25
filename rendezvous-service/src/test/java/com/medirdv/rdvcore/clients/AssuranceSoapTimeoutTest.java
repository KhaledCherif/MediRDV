package com.medirdv.rdvcore.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import com.medirdv.rdvcore.config.AssuranceSoapConfig;
import com.medirdv.rdvcore.exceptions.AssuranceServiceUnavailableException;
import com.medirdv.rdvcore.exceptions.AssureInconnuException;
import com.sun.net.httpserver.HttpServer;

/**
 * Real HTTP round trips (JDK HttpServer) to check the read timeout and the raw SOAP 1.1 fault
 * produced by assurance-legacy (HTTP 500 + SOAP-ENV:Server + faultstring "Assure inconnu : ...").
 */
class AssuranceSoapTimeoutTest {

    private static final String SOAP_FAULT = """
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
              <SOAP-ENV:Header/>
              <SOAP-ENV:Body>
                <SOAP-ENV:Fault>
                  <faultcode>SOAP-ENV:Server</faultcode>
                  <faultstring xml:lang="en">Assure inconnu : X-999</faultstring>
                </SOAP-ENV:Fault>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>""";

    private HttpServer http;
    private String baseUrl;

    @BeforeEach
    void start() throws Exception {
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/slow/ws", exchange -> {
            try {
                Thread.sleep(1_500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        http.createContext("/fault/ws", exchange -> {
            byte[] body = SOAP_FAULT.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/xml;charset=utf-8");
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        http.start();
        baseUrl = "http://127.0.0.1:" + http.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        http.stop(0);
    }

    private AssuranceSoapClient client(String path, Duration timeout) throws Exception {
        Jaxb2Marshaller marshaller = new AssuranceSoapConfig().assuranceMarshaller();
        marshaller.afterPropertiesSet();
        WebServiceTemplate template = new AssuranceSoapConfig()
                .assuranceWebServiceTemplate(marshaller, baseUrl + path, timeout);
        return new AssuranceSoapClient(template);
    }

    @Test
    void slowServerHitsTheTimeout() throws Exception {
        AssuranceSoapClient client = client("/slow/ws", Duration.ofMillis(300));

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.verifierDroits("ASSURE-001"))
                .isInstanceOf(AssuranceServiceUnavailableException.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(1_400);
    }

    @Test
    void rawAssureInconnuFaultOverHttpIsIntercepted() throws Exception {
        AssuranceSoapClient client = client("/fault/ws", Duration.ofSeconds(3));

        assertThatThrownBy(() -> client.verifierDroits("X-999"))
                .isInstanceOf(AssureInconnuException.class)
                .hasMessageContaining("X-999");
    }

    @Test
    void nobodyListeningIsUnavailable() throws Exception {
        http.stop(0);
        AssuranceSoapClient client = client("/fault/ws", Duration.ofSeconds(3));

        assertThatThrownBy(() -> client.verifierDroits("ASSURE-001"))
                .isInstanceOf(AssuranceServiceUnavailableException.class);
    }
}
