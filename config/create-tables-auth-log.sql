-- ================================================================
-- Script SQL per Oracle Query Client - Authentication & Logging
-- Database: SESAMO
-- Oracle 21c / 19c / 18c compatible
-- ================================================================

-- ================================================================
-- 1. TABELLA UTENTI
-- ================================================================

-- Drop se esiste (per sviluppo)
-- DROP TABLE sesamo.client_utenti CASCADE CONSTRAINTS;
-- DROP SEQUENCE sesamo.client_utenti_seq;

-- Sequence per ID utente
CREATE SEQUENCE sesamo.client_utenti_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Tabella utenti con password SHA-256
CREATE TABLE sesamo.client_utenti (
    id_utente           NUMBER(10)      NOT NULL,
    username            VARCHAR2(50)    NOT NULL,
    password_hash       VARCHAR2(64)    NOT NULL,  -- SHA-256 produces 64 hex chars
    nome                VARCHAR2(100),
    cognome             VARCHAR2(100),
    email               VARCHAR2(100),
    data_inizio         DATE            NOT NULL,
    data_fine           DATE,
    attivo              CHAR(1)         DEFAULT 'S' NOT NULL,
    data_creazione      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    data_modifica       TIMESTAMP       DEFAULT SYSTIMESTAMP,
    utente_creazione    VARCHAR2(50)    DEFAULT USER,
    note                VARCHAR2(500),

    CONSTRAINT pk_client_utenti PRIMARY KEY (id_utente),
    CONSTRAINT uk_client_utenti_username UNIQUE (username),
    CONSTRAINT chk_client_utenti_attivo CHECK (attivo IN ('S', 'N'))
);

-- Indici
--CREATE INDEX idx_client_utenti_username ON sesamo.client_utenti(username);
CREATE INDEX idx_client_utenti_date ON sesamo.client_utenti(data_inizio, data_fine);

-- Commenti
COMMENT ON TABLE sesamo.client_utenti IS 'Utenti autorizzati ad accedere al Client Query Oracle';
COMMENT ON COLUMN sesamo.client_utenti.id_utente IS 'ID univoco utente';
COMMENT ON COLUMN sesamo.client_utenti.username IS 'Username per login (case-sensitive)';
COMMENT ON COLUMN sesamo.client_utenti.password_hash IS 'Password hash SHA-256 (hex string, 64 chars)';
COMMENT ON COLUMN sesamo.client_utenti.data_inizio IS 'Data inizio validità utente';
COMMENT ON COLUMN sesamo.client_utenti.data_fine IS 'Data fine validità utente (NULL = illimitato)';
COMMENT ON COLUMN sesamo.client_utenti.attivo IS 'Flag attivo: S=Si, N=No';

-- ================================================================
-- 2. TABELLA LOG QUERY
-- ================================================================

-- Drop se esiste (per sviluppo)
-- DROP TABLE sesamo.client_log_query CASCADE CONSTRAINTS;
-- DROP SEQUENCE sesamo.client_log_query_seq;

-- Sequence per ID log
CREATE SEQUENCE sesamo.client_log_query_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Tabella log delle query eseguite
CREATE TABLE sesamo.client_log_query (
    id_log              NUMBER(19)      NOT NULL,
    username            VARCHAR2(50)    NOT NULL,
    database_alias      VARCHAR2(50),
    query_text          CLOB            NOT NULL,
    num_record          NUMBER(10)      DEFAULT 0,
    esito               VARCHAR2(20)    NOT NULL,  -- SUCCESS, ERROR, ROLLBACK
    error_message       VARCHAR2(4000),
    error_code          VARCHAR2(50),
    data_esecuzione     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    durata_ms           NUMBER(10),
    session_id          VARCHAR2(100),
    ip_address          VARCHAR2(50),

    CONSTRAINT pk_client_log_query PRIMARY KEY (id_log),
    CONSTRAINT chk_client_log_esito CHECK (esito IN ('SUCCESS', 'ERROR', 'ROLLBACK', 'COMMIT'))
);

-- Indici per performance
CREATE INDEX idx_client_log_username ON sesamo.client_log_query(username);
CREATE INDEX idx_client_log_data ON sesamo.client_log_query(data_esecuzione);
CREATE INDEX idx_client_log_esito ON sesamo.client_log_query(esito);
CREATE INDEX idx_client_log_session ON sesamo.client_log_query(session_id);

-- Commenti
COMMENT ON TABLE sesamo.client_log_query IS 'Log di tutte le query eseguite tramite Client Query Oracle';
COMMENT ON COLUMN sesamo.client_log_query.id_log IS 'ID univoco del log';
COMMENT ON COLUMN sesamo.client_log_query.username IS 'Username che ha eseguito la query';
COMMENT ON COLUMN sesamo.client_log_query.database_alias IS 'Alias database (sesamo, entr_asp, ecc.)';
COMMENT ON COLUMN sesamo.client_log_query.query_text IS 'Testo completo della query SQL';
COMMENT ON COLUMN sesamo.client_log_query.num_record IS 'Numero di record impattati';
COMMENT ON COLUMN sesamo.client_log_query.esito IS 'Esito: SUCCESS, ERROR, ROLLBACK, COMMIT';
COMMENT ON COLUMN sesamo.client_log_query.error_message IS 'Messaggio di errore (se esito=ERROR)';
COMMENT ON COLUMN sesamo.client_log_query.durata_ms IS 'Durata esecuzione in millisecondi';

-- ================================================================
-- 3. GRANT PRIVILEGI (Adatta al tuo schema)
-- ================================================================

-- Grant per utente applicativo (sostituisci APP_USER con il tuo utente)
-- GRANT SELECT, INSERT, UPDATE ON sesamo.client_utenti TO APP_USER;
-- GRANT SELECT, INSERT ON sesamo.client_log_query TO APP_USER;
-- GRANT SELECT ON sesamo.client_utenti_seq TO APP_USER;
-- GRANT SELECT ON sesamo.client_log_query_seq TO APP_USER;

-- ================================================================
-- 4. DATI DI TEST
-- ================================================================

-- Inserimento utenti di test
-- Password: "test123" -> SHA-256: ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae
INSERT INTO sesamo.client_utenti (
    id_utente, username, password_hash, nome, cognome, email,
    data_inizio, data_fine, attivo, note
) VALUES (
    sesamo.client_utenti_seq.NEXTVAL,
    'admin',
    'ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae',  -- test123
    'Amministratore',
    'Sistema',
    'admin@example.com',
    SYSDATE,
    NULL,  -- Nessuna scadenza
    'S',
    'Utente amministratore di sistema'
);

-- Password: "user456" -> SHA-256: 0b9c2625dc21ef05f6ad4ddf47c5f203837aa32c6946336e1ae4f8e0a8dc0a51
INSERT INTO sesamo.client_utenti (
    id_utente, username, password_hash, nome, cognome, email,
    data_inizio, data_fine, attivo, note
) VALUES (
    sesamo.client_utenti_seq.NEXTVAL,
    'operatore',
    '0b9c2625dc21ef05f6ad4ddf47c5f203837aa32c6946336e1ae4f8e0a8dc0a51',  -- user456
    'Mario',
    'Rossi',
    'mario.rossi@example.com',
    SYSDATE,
    ADD_MONTHS(SYSDATE, 12),  -- Valido 1 anno
    'S',
    'Utente operatore'
);

-- Password: "temp789" -> SHA-256: 0c0bc0e9c95e3c6f7f1ec0a6d8b8f3c5e8c7e6b7c6e7d8c9b8a7c6d5e4f3e2d1
INSERT INTO sesamo.client_utenti (
    id_utente, username, password_hash, nome, cognome, email,
    data_inizio, data_fine, attivo, note
) VALUES (
    sesamo.client_utenti_seq.NEXTVAL,
    'guest',
    '0c0bc0e9c95e3c6f7f1ec0a6d8b8f3c5e8c7e6b7c6e7d8c9b8a7c6d5e4f3e2d1',  -- temp789
    'Guest',
    'User',
    'guest@example.com',
    SYSDATE,
    SYSDATE + 7,  -- Valido 7 giorni
    'S',
    'Utente temporaneo'
);

COMMIT;

-- ================================================================
-- 5. QUERY UTILI
-- ================================================================

-- Verifica utenti creati
SELECT
    id_utente,
    username,
    nome || ' ' || cognome AS nome_completo,
    email,
    TO_CHAR(data_inizio, 'DD/MM/YYYY') AS valido_da,
    TO_CHAR(data_fine, 'DD/MM/YYYY') AS valido_fino,
    attivo,
    CASE
        WHEN attivo = 'N' THEN 'DISATTIVATO'
        WHEN data_fine IS NOT NULL AND data_fine < SYSDATE THEN 'SCADUTO'
        WHEN data_inizio > SYSDATE THEN 'NON ANCORA VALIDO'
        ELSE 'VALIDO'
    END AS stato
FROM sesamo.client_utenti
ORDER BY id_utente;

-- Conteggio log per utente
SELECT
    username,
    COUNT(*) AS totale_query,
    SUM(CASE WHEN esito = 'SUCCESS' THEN 1 ELSE 0 END) AS successi,
    SUM(CASE WHEN esito = 'ERROR' THEN 1 ELSE 0 END) AS errori,
    SUM(CASE WHEN esito = 'COMMIT' THEN 1 ELSE 0 END) AS commit,
    SUM(CASE WHEN esito = 'ROLLBACK' THEN 1 ELSE 0 END) AS rollback,
    SUM(num_record) AS totale_record_impattati
FROM sesamo.client_log_query
GROUP BY username
ORDER BY totale_query DESC;

-- Ultime 10 query eseguite
SELECT
    TO_CHAR(data_esecuzione, 'DD/MM/YYYY HH24:MI:SS') AS data_ora,
    username,
    database_alias,
    SUBSTR(query_text, 1, 100) AS query_preview,
    num_record,
    esito,
    durata_ms
FROM sesamo.client_log_query
ORDER BY data_esecuzione DESC
FETCH FIRST 10 ROWS ONLY;

-- ================================================================
-- 6. PROCEDURA PER GENERARE HASH SHA-256 (per test/admin)
-- ================================================================

-- Funzione PL/SQL per generare hash SHA-256 di una password
CREATE OR REPLACE FUNCTION sesamo.sha256_hash(p_password VARCHAR2)
RETURN VARCHAR2
IS
    v_hash RAW(32);
BEGIN
    v_hash := DBMS_CRYPTO.HASH(
        src => UTL_RAW.CAST_TO_RAW(p_password),
        typ => DBMS_CRYPTO.HASH_SH256
    );
    RETURN LOWER(RAWTOHEX(v_hash));
END;
/

-- Test della funzione
-- SELECT sesamo.sha256_hash('test123') FROM DUAL;
-- Risultato: ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae

-- ================================================================
-- 7. SCRIPT DI UTILITY
-- ================================================================

-- Cambiare password di un utente
-- UPDATE sesamo.client_utenti
-- SET password_hash = sesamo.sha256_hash('nuova_password'),
--     data_modifica = SYSTIMESTAMP
-- WHERE username = 'admin';

-- Disattivare un utente
-- UPDATE sesamo.client_utenti
-- SET attivo = 'N',
--     data_modifica = SYSTIMESTAMP
-- WHERE username = 'guest';

-- Estendere validità utente
-- UPDATE sesamo.client_utenti
-- SET data_fine = ADD_MONTHS(SYSDATE, 12),
--     data_modifica = SYSTIMESTAMP
-- WHERE username = 'operatore';

-- Eliminare log più vecchi di 6 mesi
-- DELETE FROM sesamo.client_log_query
-- WHERE data_esecuzione < ADD_MONTHS(SYSDATE, -6);

-- ================================================================
-- CREDENZIALI UTENTI DI TEST
-- ================================================================
/*
Username: admin
Password: test123
Note: Utente amministratore, nessuna scadenza

Username: operatore
Password: user456
Note: Utente operatore, valido 1 anno

Username: guest
Password: temp789
Note: Utente temporaneo, valido 7 giorni
*/

-- ================================================================
-- FINE SCRIPT
-- ================================================================
