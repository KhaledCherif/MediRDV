package com.medirdv.praticien.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grpc_request_records")
public class GrpcRequestRecord {

    @Id
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "operation_type", nullable = false, length = 16)
    private String operation;

    @Column(name = "creneau_id", nullable = false)
    private Long creneauId;

    @Column(name = "praticien_id")
    private Long praticienId;

    protected GrpcRequestRecord() {
    }

    public GrpcRequestRecord(String requestId, String operation, Long creneauId, Long praticienId) {
        this.requestId = requestId;
        this.operation = operation;
        this.creneauId = creneauId;
        this.praticienId = praticienId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getOperation() {
        return operation;
    }

    public Long getCreneauId() {
        return creneauId;
    }

    public Long getPraticienId() {
        return praticienId;
    }
}
