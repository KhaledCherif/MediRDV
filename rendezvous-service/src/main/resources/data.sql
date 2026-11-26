-- Sample rendezvous data for MediRDV
-- This runs automatically on startup when spring.jpa.hibernate.ddl-auto=update and spring.sql.init.mode=always
-- Note: patient_id and creneau_id reference data from patient-service and praticien-service
-- These sample records assume those services have been started and have IDs 1-20

-- UUIDs for idempotency (request_id)
-- Using fixed UUIDs for reproducibility

INSERT INTO rendezvous (patient_id, creneau_id, statut, request_id, pending_declaration, pending_liberation) VALUES
-- Patient 1 (Amina Ben Ali) - creneau 1 (Dr. Ahmed Ben Salah, 2025-01-15 08:00)
(1, 1, 'DROITS_A_VERIFIER', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', false, false),

-- Patient 2 (Mohamed Trabelsi) - creneau 2 (Dr. Ahmed Ben Salah, 2025-01-15 08:30)
(2, 2, 'PLANIFIE', 'b2c3d4e5-f6a7-8901-bcde-f23456789012', false, false),

-- Patient 3 (Fatma Bouazizi) - creneau 6 (Dr. Leila Mansouri, 2025-01-15 09:00)
(3, 6, 'DROITS_A_VERIFIER', 'c3d4e5f6-a7b8-9012-cdef-345678901234', false, false),

-- Patient 4 (Karim Jelassi) - creneau 11 (Dr. Mohamed Gharbi, 2025-01-15 14:00)
(4, 11, 'PLANIFIE', 'd4e5f6a7-b8c9-0123-defa-456789012345', false, false),

-- Patient 5 (Leila Messaoudi) - creneau 16 (Dr. Fatma Khelifi, 2025-01-15 10:00)
(5, 16, 'DROITS_A_VERIFIER', 'e5f6a7b8-c9d0-1234-efab-567890123456', false, false),

-- Patient 6 (Youssef Gharbi) - creneau 21 (Dr. Karim Jaziri, 2025-01-15 11:00)
(6, 21, 'PLANIFIE', 'f6a7b8c9-d0e1-2345-fabc-678901234567', false, false),

-- Patient 7 (Sarra Ben Ammar) - creneau 26 (Dr. Sarra Bouazizi, 2025-01-15 08:30)
(7, 26, 'DROITS_A_VERIFIER', 'a7b8c9d0-e1f2-3456-abcd-789012345678', false, false),

-- Patient 8 (Anis Cherni) - creneau 31 (Dr. Youssef Mhamdi, 2025-01-15 15:00)
(8, 31, 'PLANIFIE', 'b8c9d0e1-f2a3-4567-bcde-890123456789', false, false),

-- Patient 9 (Nour Ben Salah) - creneau 36 (Dr. Amina Trabelsi, 2025-01-15 08:00)
(9, 36, 'DROITS_A_VERIFIER', 'c9d0e1f2-a3b4-5678-cdef-901234567890', false, false),

-- Patient 10 (Mehdi Zaidi) - creneau 41 (Dr. Hichem Ayari, 2025-01-15 14:00)
(10, 41, 'PLANIFIE', 'd0e1f2a3-b4c5-6789-defa-012345678901', false, false),

-- Patient 11 (Imen Khlifi) - creneau 46 (Dr. Dorra Ben Hassen, 2025-01-15 09:00)
(11, 46, 'DROITS_A_VERIFIER', 'e1f2a3b4-c5d6-7890-efab-123456789012', false, false),

-- Patient 12 (Sami Bouazizi) - creneau 3 (Dr. Ahmed Ben Salah, 2025-01-15 09:00)
(12, 3, 'ANNULE', 'f2a3b4c5-d6e7-8901-fabc-234567890123', false, false),

-- Patient 13 (Hela Trabelsi) - creneau 8 (Dr. Leila Mansouri, 2025-01-15 10:00)
(13, 8, 'CLOTURE', 'a3b4c5d6-e7f8-9012-abcd-345678901234', false, false),

-- Patient 14 (Omar Ben Youssef) - creneau 13 (Dr. Mohamed Gharbi, 2025-01-15 15:00)
(14, 13, 'PLANIFIE', 'b4c5d6e7-f8a9-0123-bcde-456789012345', false, false),

-- Patient 15 (Rania Mhamdi) - creneau 18 (Dr. Fatma Khelifi, 2025-01-15 11:00)
(15, 18, 'DROITS_A_VERIFIER', 'c5d6e7f8-a9b0-1234-cdef-567890123456', false, false),

-- Patient 16 (Walid Guesmi) - creneau 23 (Dr. Karim Jaziri, 2025-01-15 12:00)
(16, 23, 'PLANIFIE', 'd6e7f8a9-b0c1-2345-defa-678901234567', false, false),

-- Patient 17 (Dorra Ben Hassen) - creneau 28 (Dr. Sarra Bouazizi, 2025-01-15 09:30)
(17, 28, 'DROITS_A_VERIFIER', 'e7f8a9b0-c1d2-3456-efab-789012345678', false, false),

-- Patient 18 (Ali Mansouri) - creneau 33 (Dr. Youssef Mhamdi, 2025-01-15 16:00)
(18, 33, 'PLANIFIE', 'f8a9b0c1-d2e3-4567-fabc-890123456789', false, false),

-- Patient 19 (Salma Jaziri) - creneau 38 (Dr. Amina Trabelsi, 2025-01-15 08:30)
(19, 38, 'ANNULE', 'a9b0c1d2-e3f4-5678-abcd-901234567890', false, false),

-- Patient 20 (Hichem Ayari) - creneau 43 (Dr. Hichem Ayari, 2025-01-15 14:45)
(20, 43, 'PLANIFIE', 'b0c1d2e3-f4a5-6789-bcde-012345678901', false, false)
ON CONFLICT (request_id) DO NOTHING;
