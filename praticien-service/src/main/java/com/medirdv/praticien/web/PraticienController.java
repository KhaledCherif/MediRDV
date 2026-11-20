package com.medirdv.praticien.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.medirdv.praticien.domain.Creneau;
import com.medirdv.praticien.domain.Praticien;
import com.medirdv.praticien.domain.StatutCreneau;
import com.medirdv.praticien.dto.CreneauRequest;
import com.medirdv.praticien.dto.CreneauResponse;
import com.medirdv.praticien.dto.PraticienRequest;
import com.medirdv.praticien.dto.PraticienResponse;
import com.medirdv.praticien.service.CreneauService;
import com.medirdv.praticien.service.PraticienService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/praticiens")
@Tag(name = "Praticiens", description = "Praticiens et leurs créneaux")
public class PraticienController {

    private final PraticienService praticiens;
    private final CreneauService creneaux;

    public PraticienController(PraticienService praticiens, CreneauService creneaux) {
        this.praticiens = praticiens;
        this.creneaux = creneaux;
    }

    @GetMapping
    @Operation(summary = "Lister les praticiens (filtre optionnel par spécialité)")
    public List<PraticienResponse> lister(@RequestParam(required = false) String specialite) {
        return praticiens.lister(specialite).stream().map(PraticienResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PraticienResponse trouver(@PathVariable Long id) {
        return PraticienResponse.from(praticiens.trouver(id));
    }

    @PostMapping
    @Operation(summary = "Créer un praticien")
    public ResponseEntity<PraticienResponse> creer(@Valid @RequestBody PraticienRequest req) {
        Praticien p = praticiens.creer(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(p.getId()).toUri();
        return ResponseEntity.created(location).body(PraticienResponse.from(p));
    }

    @PutMapping("/{id}")
    public PraticienResponse modifier(@PathVariable Long id, @Valid @RequestBody PraticienRequest req) {
        return PraticienResponse.from(praticiens.modifier(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        praticiens.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/creneaux")
    @Operation(summary = "Lister les créneaux d'un praticien (filtres statut, du, au)")
    public List<CreneauResponse> creneaux(
            @PathVariable Long id,
            @RequestParam(required = false) StatutCreneau statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime au) {
        return creneaux.lister(id, statut, du, au).stream().map(CreneauResponse::from).toList();
    }

    @PostMapping("/{id}/creneaux")
    @Operation(summary = "Créer un créneau pour un praticien")
    public ResponseEntity<CreneauResponse> creerCreneau(@PathVariable Long id,
                                                        @Valid @RequestBody CreneauRequest req) {
        Creneau c = creneaux.creer(id, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/praticiens/{pid}/creneaux/{cid}").buildAndExpand(id, c.getId()).toUri();
        return ResponseEntity.created(location).body(CreneauResponse.from(c));
    }

    @GetMapping("/{id}/creneaux/{creneauId}")
    public CreneauResponse creneau(@PathVariable Long id, @PathVariable Long creneauId) {
        Creneau c = creneaux.trouver(creneauId);
        if (!c.getPraticien().getId().equals(id)) {
            throw new com.medirdv.praticien.exception.NotFoundException(
                    "Créneau " + creneauId + " introuvable pour le praticien " + id);
        }
        return CreneauResponse.from(c);
    }
}
