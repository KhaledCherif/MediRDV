package com.medirdv.assurancelegacy;

import com.medirdv.assurancelegacy.endpoint.AssuranceEndpoint;
import com.medirdv.assurancelegacy.gen.DeclarerConsultationRequest;
import com.medirdv.assurancelegacy.gen.DeclarerConsultationResponse;
import com.medirdv.assurancelegacy.gen.ObjectFactory;
import com.medirdv.assurancelegacy.gen.VerifierDroitsRequest;
import com.medirdv.assurancelegacy.gen.VerifierDroitsResponse;
import com.medirdv.assurancelegacy.model.Assurance;
import com.medirdv.assurancelegacy.service.AssuranceService;
import com.medirdv.assurancelegacy.service.AssureInconnuException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test unitaire de l'endpoint SOAP AssuranceEndpoint.
 * Valide VerifierDroits et DeclarerConsultation, y compris l'exception AssureInconnu.
 * Aucune base de donnees requise (AssuranceService est stubbe sans Mockito,
 * pour etre compatible avec JDK 23 / ByteBuddy).
 */
class AssuranceEndpointTest {

    private AssuranceEndpoint endpoint;
    private final ObjectFactory objectFactory = new ObjectFactory();

    /**
     * Service de test qui remplace AssuranceService sans base de donnees ni Mockito.
     */
    private static class TestAssuranceService extends AssuranceService {
        public TestAssuranceService() {
            super(null);
        }

        @Override
        public Assurance verifierDroits(String numeroAssure) {
            if ("A12345678".equals(numeroAssure)) {
                Assurance a = new Assurance();
                a.setNumeroAssure("A12345678");
                a.setCouverture(true);
                a.setTauxRemboursement(new BigDecimal("0.70"));
                return a;
            }
            throw new AssureInconnuException("Assure inconnu : " + numeroAssure);
        }

        @Override
        public String declarerConsultation(String numeroAssure, String date, String praticien) {
            verifierDroits(numeroAssure); // lance AssureInconnuException si inconnu
            return "ACCUSE-" + numeroAssure + "-" + date;
        }
    }

    @BeforeEach
    void setUp() {
        AssuranceService service = new TestAssuranceService();
        endpoint = new AssuranceEndpoint(service, objectFactory);
    }

    @Test
    void verifierDroits_assureConnu_retourneCouvertureEtTaux() {
        VerifierDroitsRequest request = objectFactory.createVerifierDroitsRequest();
        request.setNumeroAssure("A12345678");

        VerifierDroitsResponse response = endpoint.verifierDroits(request);

        assertThat(response).isNotNull();
        assertThat(response.isCouverture()).isTrue();
        assertThat(response.getTaux()).isEqualByComparingTo(new BigDecimal("0.70"));
    }

    @Test
    void verifierDroits_assureInconnu_leveAssureInconnuException() {
        VerifierDroitsRequest request = objectFactory.createVerifierDroitsRequest();
        request.setNumeroAssure("INCONNU");

        assertThatThrownBy(() -> endpoint.verifierDroits(request))
                .isInstanceOf(AssureInconnuException.class);
    }

    @Test
    void declarerConsultation_assureConnu_retourneAccuse() throws Exception {
        DeclarerConsultationRequest request = objectFactory.createDeclarerConsultationRequest();
        request.setNumeroAssure("A12345678");
        XMLGregorianCalendar date = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar("2026-11-21T10:00:00");
        request.setDate(date);
        request.setPraticien("Dr. Test");

        DeclarerConsultationResponse response = endpoint.declarerConsultation(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccuse()).isNotNull();
        assertThat(response.getAccuse()).startsWith("ACCUSE-A12345678-");
    }

    @Test
    void declarerConsultation_assureInconnu_leveAssureInconnuException() throws Exception {
        DeclarerConsultationRequest request = objectFactory.createDeclarerConsultationRequest();
        request.setNumeroAssure("INCONNU");
        XMLGregorianCalendar date = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar("2026-11-21T10:00:00");
        request.setDate(date);
        request.setPraticien("Dr. Test");

        assertThatThrownBy(() -> endpoint.declarerConsultation(request))
                .isInstanceOf(AssureInconnuException.class);
    }
}
