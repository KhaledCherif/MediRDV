package com.medirdv.praticien.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medirdv.praticien.domain.Creneau;
import com.medirdv.praticien.domain.Praticien;
import com.medirdv.praticien.domain.StatutCreneau;
import com.medirdv.praticien.dto.CreneauRequest;
import com.medirdv.praticien.exception.ConflictException;
import com.medirdv.praticien.exception.NotFoundException;
import com.medirdv.praticien.repository.CreneauRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreneauService {

    private static final LocalDateTime MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(9999, 12, 31, 23, 59);

    private final CreneauRepository creneaux;
    private final PraticienService praticiens;

    public CreneauService(CreneauRepository creneaux, PraticienService praticiens) {
        this.creneaux = creneaux;
        this.praticiens = praticiens;
    }

    @Transactional(readOnly = true)
    public List<Creneau> lister(Long praticienId, StatutCreneau statut, LocalDateTime du, LocalDateTime au) {
        praticiens.trouver(praticienId);
        return creneaux.rechercher(praticienId, statut, du == null ? MIN : du, au == null ? MAX : au);
    }

    @Transactional(readOnly = true)
    public Creneau trouver(Long id) {
        return creneaux.findById(id).orElseThrow(() -> new NotFoundException("Créneau introuvable : " + id));
    }

    @Transactional
    public Creneau creer(Long praticienId, CreneauRequest req) {
        if (!req.fin().isAfter(req.debut())) {
            throw new IllegalArgumentException("fin doit être postérieure à debut");
        }
        Praticien p = praticiens.trouver(praticienId);
        if (creneaux.existsByPraticienIdAndDebutLessThanAndFinGreaterThan(praticienId, req.fin(), req.debut())) {
            throw new ConflictException("Le créneau chevauche un créneau existant de ce praticien");
        }
        return creneaux.save(new Creneau(p, req.debut(), req.fin()));
    }

    /**
     * Réservation atomique : un seul UPDATE ... WHERE statut = 'LIBRE'.
     * 0 ligne modifiée = créneau inexistant (404) ou déjà pris (409). Aucune lecture préalable ne décide du résultat,
     * donc pas de fenêtre de course.
     */
    @Transactional
    public void reserver(Long id) {
        if (creneaux.reserverSiLibre(id) == 1) {
            return;
        }
        if (!creneaux.existsById(id)) {
            throw new NotFoundException("Créneau introuvable : " + id);
        }
        throw new ConflictException("Créneau déjà réservé : " + id);
    }

    /** Compensation : idempotente par nature (libérer un créneau déjà libre est un no-op). */
    @Transactional
    public void liberer(Long id) {
        if (creneaux.libererSiReserve(id) == 0 && !creneaux.existsById(id)) {
            throw new NotFoundException("Créneau introuvable : " + id);
        }
    }
}
