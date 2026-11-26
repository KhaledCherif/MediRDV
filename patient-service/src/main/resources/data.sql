-- Sample patient data for MediRDV
-- This runs automatically on startup when spring.jpa.hibernate.ddl-auto=update and spring.sql.init.mode=always

INSERT INTO patients (nom, contact, numero_assure) VALUES
('Amina Ben Ali', '+216 20 000 001', 'ASSURE-001'),
('Mohamed Trabelsi', '+216 20 000 002', 'ASSURE-002'),
('Fatma Bouazizi', '+216 20 000 003', 'ASSURE-003'),
('Karim Jelassi', '+216 20 000 004', 'ASSURE-004'),
('Leila Messaoudi', '+216 20 000 005', 'ASSURE-005'),
('Youssef Gharbi', '+216 20 000 006', 'ASSURE-006'),
('Sarra Ben Ammar', '+216 20 000 007', 'ASSURE-007'),
('Anis Cherni', '+216 20 000 008', 'ASSURE-008'),
('Nour Ben Salah', '+216 20 000 009', 'ASSURE-009'),
('Mehdi Zaidi', '+216 20 000 010', 'ASSURE-010'),
('Imen Khlifi', '+216 20 000 011', 'ASSURE-011'),
('Sami Bouazizi', '+216 20 000 012', 'ASSURE-012'),
('Hela Trabelsi', '+216 20 000 013', 'ASSURE-013'),
('Omar Ben Youssef', '+216 20 000 014', 'ASSURE-014'),
('Rania Mhamdi', '+216 20 000 015', 'ASSURE-015'),
('Walid Guesmi', '+216 20 000 016', 'ASSURE-016'),
('Dorra Ben Hassen', '+216 20 000 017', 'ASSURE-017'),
('Ali Mansouri', '+216 20 000 018', 'ASSURE-018'),
('Salma Jaziri', '+216 20 000 019', 'ASSURE-019'),
('Hichem Ayari', '+216 20 000 020', 'ASSURE-020')
ON CONFLICT (numero_assure) DO NOTHING;
