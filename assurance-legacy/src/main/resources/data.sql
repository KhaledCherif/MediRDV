-- Sample assurance data for MediRDV
-- This runs automatically on startup when spring.jpa.hibernate.ddl-auto=update and spring.sql.init.mode=always
-- Matches patient-service numero_assure values for integration testing

INSERT INTO assurances (numero_assure, nom, prenom, couverture, taux_remboursement, date_creation) VALUES
('ASSURE-001', 'Ben Ali', 'Amina', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-002', 'Trabelsi', 'Mohamed', true, 80.00, '2024-01-15 10:00:00'),
('ASSURE-003', 'Bouazizi', 'Fatma', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-004', 'Jelassi', 'Karim', true, 90.00, '2024-01-15 10:00:00'),
('ASSURE-005', 'Messaoudi', 'Leila', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-006', 'Gharbi', 'Youssef', false, 0.00, '2024-01-15 10:00:00'),
('ASSURE-007', 'Ben Ammar', 'Sarra', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-008', 'Cherni', 'Anis', true, 80.00, '2024-01-15 10:00:00'),
('ASSURE-009', 'Ben Salah', 'Nour', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-010', 'Zaidi', 'Mehdi', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-011', 'Khlifi', 'Imen', true, 85.00, '2024-01-15 10:00:00'),
('ASSURE-012', 'Bouazizi', 'Sami', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-013', 'Trabelsi', 'Hela', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-014', 'Ben Youssef', 'Omar', true, 90.00, '2024-01-15 10:00:00'),
('ASSURE-015', 'Mhamdi', 'Rania', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-016', 'Guesmi', 'Walid', false, 0.00, '2024-01-15 10:00:00'),
('ASSURE-017', 'Ben Hassen', 'Dorra', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-018', 'Mansouri', 'Ali', true, 80.00, '2024-01-15 10:00:00'),
('ASSURE-019', 'Jaziri', 'Salma', true, 70.00, '2024-01-15 10:00:00'),
('ASSURE-020', 'Ayari', 'Hichem', true, 70.00, '2024-01-15 10:00:00'),

-- Additional test records with different coverage levels
('ASSURE-TEST-001', 'Test', 'Couverture100', true, 100.00, '2024-01-15 10:00:00'),
('ASSURE-TEST-002', 'Test', 'Couverture50', true, 50.00, '2024-01-15 10:00:00'),
('ASSURE-TEST-003', 'Test', 'SansCouverture', false, 0.00, '2024-01-15 10:00:00')
ON CONFLICT (numero_assure) DO NOTHING;
