package com.medirdv.patientservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "patients")
public class Patient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Identifiant généré automatiquement")
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String nom;

	@NotBlank
	@Column(nullable = false)
	private String contact;

	@NotBlank
	@Column(nullable = false, unique = true)
	private String numeroAssure;

	public Patient() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getNumeroAssure() {
		return numeroAssure;
	}

	public void setNumeroAssure(String numeroAssure) {
		this.numeroAssure = numeroAssure;
	}
}
