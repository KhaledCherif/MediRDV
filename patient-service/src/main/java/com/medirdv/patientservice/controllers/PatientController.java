package com.medirdv.patientservice.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.medirdv.patientservice.entities.Patient;
import com.medirdv.patientservice.services.PatientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Gestion des dossiers patients")
public class PatientController {

	private final PatientService patientService;

	public PatientController(PatientService patientService) {
		this.patientService = patientService;
	}

	@GetMapping
	@Operation(summary = "Lister les patients")
	@ApiResponse(responseCode = "200", description = "Liste des patients renvoyée")
	public List<Patient> findAll() {
		return patientService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Rechercher un patient par identifiant")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Patient trouvé", content = @Content(schema = @Schema(implementation = Patient.class))),
			@ApiResponse(responseCode = "404", description = "Patient introuvable", content = @Content)
	})
	public Patient findById(@PathVariable Long id) {
		return patientService.findById(id);
	}

	@GetMapping("/numero-assure/{numeroAssure}")
	@Operation(summary = "Rechercher un patient par numéro d'assuré")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Patient trouvé", content = @Content(schema = @Schema(implementation = Patient.class))),
			@ApiResponse(responseCode = "404", description = "Patient introuvable", content = @Content)
	})
	public Patient findByNumeroAssure(@PathVariable String numeroAssure) {
		return patientService.findByNumeroAssure(numeroAssure);
	}

	@PostMapping
	@Operation(summary = "Créer un dossier patient")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Patient créé", content = @Content(schema = @Schema(implementation = Patient.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
			@ApiResponse(responseCode = "409", description = "Numéro d'assuré déjà utilisé", content = @Content)
	})
	public ResponseEntity<Patient> create(@Valid @RequestBody Patient patient) {
		Patient createdPatient = patientService.create(patient);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(createdPatient.getId())
				.toUri();
		return ResponseEntity.created(location).body(createdPatient);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Modifier un dossier patient")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Patient modifié", content = @Content(schema = @Schema(implementation = Patient.class))),
			@ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
			@ApiResponse(responseCode = "404", description = "Patient introuvable", content = @Content),
			@ApiResponse(responseCode = "409", description = "Numéro d'assuré déjà utilisé", content = @Content)
	})
	public Patient update(@PathVariable Long id, @Valid @RequestBody Patient patient) {
		return patientService.update(id, patient);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Supprimer un dossier patient")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Patient supprimé"),
			@ApiResponse(responseCode = "404", description = "Patient introuvable", content = @Content)
	})
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		patientService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
