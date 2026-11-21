package com.medirdv.assurancelegacy.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entite Assurance du service assurance-legacy.
 * Persistee en PostgreSQL (pas de H2).
 */
@Entity
@Table(name = "assurances")
public class Assurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_assure", nullable = false, unique = true, length = 50)
    private String numeroAssure;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false)
    private Boolean couverture = true;

    @Column(name = "taux_remboursement", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxRemboursement = new BigDecimal("0.70");

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Constructeurs
    public Assurance() {
    }

    public Assurance(String numeroAssure, String nom, String prenom, Boolean couverture,
                     BigDecimal tauxRemboursement) {
        this.numeroAssure = numeroAssure;
        this.nom = nom;
        this.prenom = prenom;
        this.couverture = couverture;
        this.tauxRemboursement = tauxRemboursement;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroAssure() {
        return numeroAssure;
    }

    public void setNumeroAssure(String numeroAssure) {
        this.numeroAssure = numeroAssure;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Boolean getCouverture() {
        return couverture;
    }

    public void setCouverture(Boolean couverture) {
        this.couverture = couverture;
    }

    public BigDecimal getTauxRemboursement() {
        return tauxRemboursement;
    }

    public void setTauxRemboursement(BigDecimal tauxRemboursement) {
        this.tauxRemboursement = tauxRemboursement;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
