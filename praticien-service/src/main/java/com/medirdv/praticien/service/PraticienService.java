package com.medirdv.praticien.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medirdv.praticien.domain.Praticien;
import com.medirdv.praticien.dto.PraticienRequest;
import com.medirdv.praticien.exception.NotFoundException;
import com.medirdv.praticien.repository.PraticienRepository;

import java.util.List;

@Service
public class PraticienService {

    private final PraticienRepository repository;

    public PraticienService(PraticienRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Praticien> lister(String specialite) {
        return (specialite == null || specialite.isBlank())
                ? repository.findAll()
                : repository.findBySpecialiteIgnoreCase(specialite);
    }

    @Transactional(readOnly = true)
    public Praticien trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Praticien introuvable : " + id));
    }

    @Transactional
    public Praticien creer(PraticienRequest req) {
        return repository.save(new Praticien(req.nom(), req.specialite()));
    }

    @Transactional
    public Praticien modifier(Long id, PraticienRequest req) {
        Praticien p = trouver(id);
        p.setNom(req.nom());
        p.setSpecialite(req.specialite());
        return p;
    }

    @Transactional
    public void supprimer(Long id) {
        repository.delete(trouver(id));
    }
}
