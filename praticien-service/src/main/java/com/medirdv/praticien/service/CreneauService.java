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

    @Transactional(readOnly = true)
    public Creneau trouver(Long praticienId, Long id) {
        return trouverPourPraticien(praticienId, id);
    }

    @Transactional
    public Creneau creer(Long praticienId, CreneauRequest req) {
        verifierPeriode(req);
        Praticien p = praticiens.trouver(praticienId);
        if (creneaux.existsByPraticienIdAndDebutLessThanAndFinGreaterThan(praticienId, req.fin(), req.debut())) {
            throw new ConflictException("Le créneau chevauche un créneau existant de ce praticien");
        }
        return creneaux.save(new Creneau(p, req.debut(), req.fin()));
    }

    @Transactional
    public Creneau modifier(Long praticienId, Long id, CreneauRequest req) {
        verifierPeriode(req);
        Creneau c = trouverPourPraticien(praticienId, id);
        if (c.getStatut() != StatutCreneau.LIBRE) {
            throw new ConflictException("Seul un créneau libre peut être modifié");
        }
        if (creneaux.existsByPraticienIdAndIdNotAndDebutLessThanAndFinGreaterThan(
                praticienId, id, req.fin(), req.debut())) {
            throw new ConflictException("Le créneau chevauche un créneau existant de ce praticien");
        }
        c.setDebut(req.debut());
        c.setFin(req.fin());
        return c;
    }

    @Transactional
    public void supprimer(Long praticienId, Long id) {
        Creneau c = trouverPourPraticien(praticienId, id);
        if (c.getStatut() != StatutCreneau.LIBRE) {
            throw new ConflictException("Seul un créneau libre peut être supprimé");
        }
        creneaux.delete(c);
    }

    @Transactional
    public Creneau accepter(Long praticienId, Long id) {
        return traiterDemande(praticienId, id, creneaux::accepterSiEnAttente);
    }

    @Transactional
    public Creneau refuser(Long praticienId, Long id) {
        return traiterDemande(praticienId, id, creneaux::refuserSiEnAttente);
    }

    /**
     * Réservation atomique : un seul UPDATE ... WHERE statut = 'LIBRE', la demande passe en attente.
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

    /** Compensation : libère une demande en attente ou un créneau confirmé. */
    @Transactional
    public void liberer(Long id) {
        if (creneaux.libererSiEnAttenteOuReserve(id) == 0 && !creneaux.existsById(id)) {
            throw new NotFoundException("Créneau introuvable : " + id);
        }
    }

    private Creneau traiterDemande(Long praticienId, Long id, java.util.function.ToIntFunction<Long> transition) {
        trouverPourPraticien(praticienId, id);
        if (transition.applyAsInt(id) == 0) {
            throw new ConflictException("Le créneau n'a pas de demande en attente");
        }
        return trouver(id);
    }

    private Creneau trouverPourPraticien(Long praticienId, Long id) {
        Creneau c = trouver(id);
        if (!c.getPraticien().getId().equals(praticienId)) {
            throw new NotFoundException("Créneau " + id + " introuvable pour le praticien " + praticienId);
        }
        return c;
    }

    private void verifierPeriode(CreneauRequest req) {
        if (!req.fin().isAfter(req.debut())) {
            throw new IllegalArgumentException("fin doit être postérieure à debut");
        }
    }
}
