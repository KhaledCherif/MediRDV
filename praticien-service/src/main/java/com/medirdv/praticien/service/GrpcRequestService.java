package com.medirdv.praticien.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.medirdv.praticien.domain.GrpcRequestRecord;
import com.medirdv.praticien.exception.ConflictException;
import com.medirdv.praticien.exception.NotFoundException;
import com.medirdv.praticien.repository.GrpcRequestRecordRepository;

import java.util.Objects;
import java.util.UUID;

@Service
public class GrpcRequestService {

    private static final String RESERVER = "RESERVER";
    private static final String LIBERER = "LIBERER";

    private final CreneauService creneaux;
    private final GrpcRequestRecordRepository requests;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readTransaction;

    public GrpcRequestService(CreneauService creneaux, GrpcRequestRecordRepository requests,
                              PlatformTransactionManager transactionManager) {
        this.creneaux = creneaux;
        this.requests = requests;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
        this.readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public ReservationResult reserver(String requestId, long creneauId) {
        String normalizedRequestId = normaliserRequest(requestId, creneauId);
        try {
            return Objects.requireNonNull(writeTransaction.execute(
                    status -> reserverDansTransaction(normalizedRequestId, creneauId)));
        } catch (DataIntegrityViolationException collision) {
            GrpcRequestRecord record = lireApresCollision(normalizedRequestId, RESERVER, creneauId, collision);
            return new ReservationResult(record.getPraticienId(), true);
        }
    }

    public void liberer(String requestId, long creneauId) {
        String normalizedRequestId = normaliserRequest(requestId, creneauId);
        try {
            writeTransaction.executeWithoutResult(
                    status -> libererDansTransaction(normalizedRequestId, creneauId));
        } catch (DataIntegrityViolationException collision) {
            lireApresCollision(normalizedRequestId, LIBERER, creneauId, collision);
        }
    }

    private ReservationResult reserverDansTransaction(String requestId, long creneauId) {
        GrpcRequestRecord existing = requests.findById(requestId).orElse(null);
        if (existing != null) {
            validerRejeu(existing, RESERVER, creneauId);
            return new ReservationResult(existing.getPraticienId(), true);
        }

        if (!creneaux.tenterReserver(creneauId)) {
            existing = requests.findById(requestId).orElse(null);
            if (existing != null) {
                validerRejeu(existing, RESERVER, creneauId);
                return new ReservationResult(existing.getPraticienId(), true);
            }
            if (!creneaux.existe(creneauId)) {
                throw new NotFoundException("Créneau introuvable : " + creneauId);
            }
            throw new ConflictException("Créneau déjà réservé : " + creneauId);
        }

        Long praticienId = creneaux.trouver(creneauId).getPraticien().getId();
        requests.save(new GrpcRequestRecord(requestId, RESERVER, creneauId, praticienId));
        return new ReservationResult(praticienId, false);
    }

    private void libererDansTransaction(String requestId, long creneauId) {
        GrpcRequestRecord existing = requests.findById(requestId).orElse(null);
        if (existing != null) {
            validerRejeu(existing, LIBERER, creneauId);
            return;
        }

        creneaux.liberer(creneauId);
        requests.save(new GrpcRequestRecord(requestId, LIBERER, creneauId, null));
    }

    private GrpcRequestRecord lireApresCollision(String requestId, String operation, long creneauId,
                                                  DataIntegrityViolationException collision) {
        GrpcRequestRecord existing = readTransaction.execute(status -> requests.findById(requestId).orElse(null));
        if (existing == null) {
            throw collision;
        }
        validerRejeu(existing, operation, creneauId);
        return existing;
    }

    private void validerRejeu(GrpcRequestRecord record, String operation, long creneauId) {
        if (!record.getOperation().equals(operation) || !record.getCreneauId().equals(creneauId)) {
            throw new IllegalArgumentException("request_id déjà utilisé pour une autre opération ou un autre créneau");
        }
    }

    private String normaliserRequest(String requestId, long creneauId) {
        if (creneauId <= 0) {
            throw new IllegalArgumentException("creneau_id doit être positif");
        }
        if (requestId == null || requestId.length() != 36) {
            throw new IllegalArgumentException("request_id doit être un UUID valide");
        }
        try {
            String normalized = UUID.fromString(requestId).toString();
            if (!normalized.equalsIgnoreCase(requestId)) {
                throw new IllegalArgumentException("request_id doit être un UUID valide");
            }
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("request_id doit être un UUID valide", ex);
        }
    }

    public record ReservationResult(Long praticienId, boolean dejaTraite) {
    }
}
