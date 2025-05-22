-- Création de la table des utilisateurs
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(10) CHECK (type IN ('STUDENT', 'TEACHER', 'ADMIN')),
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    class_name VARCHAR(50),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);

-- Création de la table des salles
CREATE TABLE rooms (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(10) UNIQUE NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    type VARCHAR(25) NOT NULL CHECK (type IN ('Amphithéâtre', 'Salle de cours', 'Laboratoire', 'Salle informatique'))
);

-- Création de la table des matières
CREATE TABLE subjects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- Création de la table de l'emploi du temps
CREATE TABLE schedules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id INTEGER NOT NULL,
    teacher_id INTEGER NOT NULL,
    room_id INTEGER NOT NULL,
    course_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    class_name TEXT NOT NULL CHECK (class_name IN ('P1', 'P2', 'A1', 'A2', 'A3')),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    CHECK (start_time < end_time)
);

-- Création de la table des notifications
CREATE TABLE notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    recipient_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    sender_id INTEGER NOT NULL,
    date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT 0,
    FOREIGN KEY (recipient_id) REFERENCES users(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- Création d'un index pour optimiser la recherche des cours par date
CREATE INDEX idx_schedules_date ON schedules(course_date);

-- Création d'un index pour optimiser la recherche des notifications par destinataire
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id); 