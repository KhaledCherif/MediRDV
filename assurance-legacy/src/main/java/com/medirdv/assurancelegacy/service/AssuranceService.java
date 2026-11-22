package com.medirdv.assurancelegacy.service;

import com.medirdv.assurancelegacy.model.Assurance;
import com.medirdv.assurancelegacy.repository.AssuranceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssuranceService {

    private final AssuranceRepository assuranceRepository;

    public AssuranceService(AssuranceRepository assuranceRepository) {
        this.assuranceRepository = assuranceRepository;
    }

    /**
     * Verifie les droits d'un assure.
     * @throws AssureInconnuException si l'assure n'existe pas
     */
    public Assurance verifierDroits(String numeroAssure) {
        return assuranceRepository.findByNumeroAssure(numeroAssure)
                .orElseThrow(() -> new AssureInconnuException("Assure inconnu : " + numeroAssure));
    }

    /**
     * Declare une consultation pour un assure.
     * @throws AssureInconnuException si l'assure n'existe pas
     */
    @Transactional
    public String declarerConsultation(String numeroAssure, String date, String praticien) {
        // Verifie que l'assure existe (lance AssureInconnuException sinon)
        verifierDroits(numeroAssure);
        return "ACCUSE-" + numeroAssure + "-" + date;
    }
}
