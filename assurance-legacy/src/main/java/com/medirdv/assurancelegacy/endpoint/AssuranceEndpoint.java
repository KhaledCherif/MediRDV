package com.medirdv.assurancelegacy.endpoint;

import com.medirdv.assurancelegacy.gen.ObjectFactory;
import com.medirdv.assurancelegacy.gen.DeclarerConsultationRequest;
import com.medirdv.assurancelegacy.gen.DeclarerConsultationResponse;
import com.medirdv.assurancelegacy.gen.VerifierDroitsRequest;
import com.medirdv.assurancelegacy.gen.VerifierDroitsResponse;
import com.medirdv.assurancelegacy.service.AssuranceService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;


@Endpoint
public class AssuranceEndpoint {

    public static final String NAMESPACE_URI = "http://medirdv.com/assurance-legacy";

    private final AssuranceService assuranceService;
    private final ObjectFactory objectFactory;

    public AssuranceEndpoint(AssuranceService assuranceService, ObjectFactory objectFactory) {
        this.assuranceService = assuranceService;
        this.objectFactory = objectFactory;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "verifierDroitsRequest")
    @ResponsePayload
    public VerifierDroitsResponse verifierDroits(@RequestPayload VerifierDroitsRequest request) {
        var assurance = assuranceService.verifierDroits(request.getNumeroAssure());

        VerifierDroitsResponse response = objectFactory.createVerifierDroitsResponse();
        response.setCouverture(assurance.getCouverture());
        response.setTaux(assurance.getTauxRemboursement());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "declarerConsultationRequest")
    @ResponsePayload
    public DeclarerConsultationResponse declarerConsultation(@RequestPayload DeclarerConsultationRequest request) {
        assuranceService.declarerConsultation(
                request.getNumeroAssure(),
                request.getDate().toString(),
                request.getPraticien()
        );

        DeclarerConsultationResponse response = objectFactory.createDeclarerConsultationResponse();
        response.setAccuse("ACCUSE-" + request.getNumeroAssure() + "-" + request.getDate());
        return response;
    }
}
