-- The specializations a doctor can choose from. Rows change only through migrations, so their ids are stable.
CREATE TABLE specializations
(
    id   INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO specializations (id, name)
VALUES (1, 'Allergist'),
       (2, 'Anesthesiologist'),
       (3, 'Cardiologist'),
       (4, 'Dentist'),
       (5, 'Dermatologist'),
       (6, 'Endocrinologist'),
       (7, 'Gastroenterologist'),
       (8, 'General practitioner'),
       (9, 'Gynecologist'),
       (10, 'Hematologist'),
       (11, 'Infectious disease specialist'),
       (12, 'Nephrologist'),
       (13, 'Neurologist'),
       (14, 'Oncologist'),
       (15, 'Ophthalmologist'),
       (16, 'Orthopedist'),
       (17, 'Otolaryngologist'),
       (18, 'Pediatrician'),
       (19, 'Psychiatrist'),
       (20, 'Pulmonologist'),
       (21, 'Radiologist'),
       (22, 'Rheumatologist'),
       (23, 'Surgeon'),
       (24, 'Urologist');
