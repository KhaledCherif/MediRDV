package com.medirdv.praticien.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "creneaux", indexes = {
        @Index(name = "idx_creneau_praticien_debut", columnList = "praticien_id, debut"),
        @Index(name = "idx_creneau_statut_debut", columnList = "statut, debut")
})
public class Creneau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "praticien_id", nullable = false)
    private Praticien praticien;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutCreneau statut = StatutCreneau.LIBRE;

    @Version
    private Long version;

    protected Creneau() {
    }

    public Creneau(Praticien praticien, LocalDateTime debut, LocalDateTime fin) {
        this.praticien = praticien;
        this.debut = debut;
        this.fin = fin;
        this.statut = StatutCreneau.LIBRE;
    }

    public Long getId() { return id; }
    public Praticien getPraticien() { return praticien; }
    public LocalDateTime getDebut() { return debut; }
    public void setDebut(LocalDateTime debut) { this.debut = debut; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
    public StatutCreneau getStatut() { return statut; }
    public Long getVersion() { return version; }
}
