package com.medirdv.praticien.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.medirdv.praticien.domain.GrpcRequestRecord;

public interface GrpcRequestRecordRepository extends JpaRepository<GrpcRequestRecord, String> {
}
