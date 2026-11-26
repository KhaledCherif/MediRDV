package com.medirdv.rdvcore.entities;

import java.util.UUID;

import com.medirdv.rdvcore.enums.RendezvousStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "rendezvous")
public class Rendezvous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "creneau_id", nullable = false)
    private Long creneauId;

    // Nullable for appointments created before Suivi 2 orchestration.
    @Column(name = "praticien_id")
    private Long praticienId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private RendezvousStatus statut;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "pending_declaration", nullable = false)
    private boolean pendingDeclaration = false;

    @Column(name = "pending_liberation", nullable = false)
    private boolean pendingLiberation = false;

    public Rendezvous() {
    }

    public Rendezvous(Long patientId, Long creneauId, RendezvousStatus statut, UUID requestId) {
        this.patientId = patientId;
        this.creneauId = creneauId;
        this.statut = statut;
        this.requestId = requestId;
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getCreneauId() {
        return creneauId;
    }

    public void setCreneauId(Long creneauId) {
        this.creneauId = creneauId;
    }

    public Long getPraticienId() {
        return praticienId;
    }

    public void setPraticienId(Long praticienId) {
        this.praticienId = praticienId;
    }

    public RendezvousStatus getStatut() {
        return statut;
    }

    public void setStatut(RendezvousStatus statut) {
        this.statut = statut;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public boolean isPendingDeclaration() {
        return pendingDeclaration;
    }

    public void setPendingDeclaration(boolean pendingDeclaration) {
        this.pendingDeclaration = pendingDeclaration;
    }

    public boolean isPendingLiberation() {
        return pendingLiberation;
    }

    public void setPendingLiberation(boolean pendingLiberation) {
        this.pendingLiberation = pendingLiberation;
    }
}
