package com.medirdv.praticien.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.medirdv.praticien.domain.Praticien;

import java.util.List;

public interface PraticienRepository extends JpaRepository<Praticien, Long> {
    List<Praticien> findBySpecialiteIgnoreCase(String specialite);
}
