package com.medirdv.rdvcore.controllers;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.medirdv.rdvcore.dto.CreateRendezvousRequest;
import com.medirdv.rdvcore.dto.CreneauxDisponibles;
import com.medirdv.rdvcore.dto.RendezvousResponse;
import com.medirdv.rdvcore.dto.UpdateStatutRequest;
import com.medirdv.rdvcore.services.RendezvousService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rendezvous")
@Tag(name = "Rendez-vous", description = "Gestion des rendez-vous")
public class RendezvousController {

    private final RendezvousService rendezvousService;

    public RendezvousController(RendezvousService rendezvousService) {
        this.rendezvousService = rendezvousService;
    }

    @PostMapping
    @Operation(summary = "Créer un rendez-vous (idempotent sur requestId)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rendez-vous créé"),
            @ApiResponse(responseCode = "200", description = "requestId déjà traité : rendez-vous existant renvoyé"),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
            @ApiResponse(responseCode = "404", description = "Patient ou créneau introuvable", content = @Content),
            @ApiResponse(responseCode = "409", description = "Créneau pris ou requestId réutilisé", content = @Content),
            @ApiResponse(responseCode = "422", description = "Assuré inconnu ou non couvert", content = @Content),
            @ApiResponse(responseCode = "503", description = "Service distant ou base indisponible", content = @Content)
    })
    public ResponseEntity<RendezvousResponse> create(@Valid @RequestBody CreateRendezvousRequest request) {
        RendezvousService.CreationResult result = rendezvousService.create(request);
        RendezvousResponse body = RendezvousResponse.from(result.rendezvous());
        if (!result.created()) {
            return ResponseEntity.ok(body);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un rendez-vous")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rendez-vous trouvé"),
            @ApiResponse(responseCode = "404", description = "Rendez-vous introuvable", content = @Content)
    })
    public RendezvousResponse findById(@PathVariable Long id) {
        return RendezvousResponse.from(rendezvousService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lister les rendez-vous (filtre optionnel par patient)")
    @ApiResponse(responseCode = "200", description = "Liste renvoyée")
    public List<RendezvousResponse> findAll(@RequestParam(required = false) Long patientId) {
        return rendezvousService.findAll(patientId).stream()
                .map(RendezvousResponse::from)
                .toList();
    }

    @GetMapping("/creneaux-disponibles")
    @Operation(summary = "Lister les créneaux disponibles pour un patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Créneaux filtrés, avec indicateur de résultat partiel"),
            @ApiResponse(responseCode = "400", description = "Filtres invalides", content = @Content),
            @ApiResponse(responseCode = "503", description = "praticien-service indisponible", content = @Content)
    })
    public CreneauxDisponibles availableSlots(
            @RequestParam Long patientId,
            @RequestParam(required = false) String specialite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime au) {
        return rendezvousService.availableSlotsForPatient(patientId, specialite, du, au);
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Annuler ou clôturer un rendez-vous")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié"),
            @ApiResponse(responseCode = "400", description = "Statut invalide", content = @Content),
            @ApiResponse(responseCode = "404", description = "Rendez-vous introuvable", content = @Content),
            @ApiResponse(responseCode = "409", description = "Transition interdite", content = @Content),
            @ApiResponse(responseCode = "422", description = "Assuré inconnu", content = @Content),
            @ApiResponse(responseCode = "503", description = "Libération ou métadonnées indisponibles", content = @Content)
    })
    public RendezvousResponse changeStatut(@PathVariable Long id,
                                           @Valid @RequestBody UpdateStatutRequest request) {
        return RendezvousResponse.from(rendezvousService.changeStatut(id, request.statut()));
    }
}
