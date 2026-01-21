# Oracle Query Client - Esempi di Utilizzo

Questa guida mostra come utilizzare l'applicazione con entrambi i metodi di selezione database.

## 🎯 Metodo 1: Auto-Detection (Consigliato)

Il database viene rilevato automaticamente dalla query usando il formato `schema.table`.

### Esempio 1: UPDATE con SESAMO

**Query:**
```sql
UPDATE sesamo.uffici
SET data_fine = SYSDATE
WHERE id_ufficio = 111;
```

**Come funziona:**
1. Lascia il dropdown "Database Alias" su "-- Auto-detect from query --"
2. Inserisci la query sopra
3. Click su "Execute Query"
4. L'applicazione **estrae automaticamente** "sesamo" dalla query
5. Usa il JNDI: `jdbc/ds_sesamo`

### Esempio 2: INSERT con ENTR_ASP

**Query:**
```sql
INSERT INTO entr_asp.audit_log (id, action, timestamp, user_name)
VALUES (audit_seq.NEXTVAL, 'User Login', SYSDATE, USER);
```

**Come funziona:**
- L'applicazione rileva "entr_asp" dalla query
- Usa automaticamente JNDI: `jdbc/nsd_entr`

### Esempio 3: DELETE con SESAMO

**Query:**
```sql
DELETE FROM sesamo.temp_records
WHERE created_date < SYSDATE - 30;
```

**Come funziona:**
- Rileva "sesamo" → usa `jdbc/ds_sesamo`

## 🎯 Metodo 2: Selezione Manuale

Puoi anche selezionare manualmente il database dal dropdown.

### Esempio 4: Selezione Manuale

**Steps:**
1. Seleziona "**sesamo (jdbc/ds_sesamo)**" dal dropdown
2. Inserisci la query (anche senza schema qualifier):
   ```sql
   UPDATE uffici
   SET data_fine = SYSDATE
   WHERE id_ufficio = 111;
   ```
3. Click "Execute Query"
4. Usa il database selezionato, indipendentemente dalla query

**Nota:** Se specifichi sia il dropdown che lo schema nella query, il **dropdown ha la precedenza**.

## 📋 Mapping Alias → JNDI Attuale

| Alias | JNDI Name | Esempio Query |
|-------|-----------|---------------|
| `entr_asp` | `jdbc/nsd_entr` | `UPDATE entr_asp.tabella SET ...` |
| `sesamo` | `jdbc/ds_sesamo` | `UPDATE sesamo.uffici SET ...` |

## ⚡ Best Practices

### ✅ Raccomandato

1. **Usa sempre schema.table nelle query** per chiarezza:
   ```sql
   UPDATE sesamo.uffici SET data_fine = SYSDATE WHERE id_ufficio = 111;
   ```

2. **Lascia il dropdown su "Auto-detect"** per maggiore flessibilità

3. **Verifica sempre il numero di record impattati** prima del commit

4. **Testa prima su database di sviluppo/test**

### ❌ Da Evitare

1. **NON mescolare alias diversi nella stessa query:**
   ```sql
   -- ❌ NON FARE QUESTO - Ambiguo!
   UPDATE sesamo.uffici u
   SET u.data_fine = (SELECT MAX(data) FROM entr_asp.storico);
   ```
   → Usa una sola connessione per query

2. **NON eseguire UPDATE/DELETE senza WHERE:**
   ```sql
   -- ❌ PERICOLOSO!
   UPDATE sesamo.uffici SET data_fine = SYSDATE;
   ```
   → Impatta TUTTI i record!

3. **NON fare commit senza verificare "Affected Rows"**

## 🔄 Workflow Completo

### Esempio Completo: Aggiornamento Uffici

**Scenario:** Devi chiudere l'ufficio con ID 111 nel database SESAMO.

**Step 1: Esegui Query**
```sql
UPDATE sesamo.uffici
SET data_fine = SYSDATE,
    stato = 'CHIUSO'
WHERE id_ufficio = 111;
```

**Step 2: Verifica Risultato**
- ✅ Status: SUCCESS
- 📊 Affected Rows: **1**
- 💬 Message: "Query executed successfully. 1 row(s) affected. Transaction pending - please commit or rollback."

**Step 3: Commit o Rollback**

**Se tutto è corretto:**
- Click "✓ Commit Changes"
- Le modifiche diventano permanenti

**Se hai fatto un errore:**
- Click "↩ Rollback Changes"
- Le modifiche vengono annullate

## 🐛 Troubleshooting

### Errore: "Cannot determine database alias"

**Causa:** La query non contiene schema qualifier e nessun alias selezionato.

**Soluzione:**
```sql
-- ❌ Errore:
UPDATE uffici SET data_fine = SYSDATE WHERE id = 111;

-- ✅ Corretto:
UPDATE sesamo.uffici SET data_fine = SYSDATE WHERE id = 111;
```

### Errore: "Invalid database alias: xyz"

**Causa:** Schema "xyz" non è configurato nei database alias.

**Soluzione:** Usa uno degli alias configurati:
- `entr_asp`
- `sesamo`

O contatta l'amministratore per aggiungere il nuovo alias.

### Errore: "Cannot get connection from datasource"

**Causa:** Il datasource JNDI non è configurato in WebSphere.

**Soluzione:** Verifica che il datasource sia configurato:
1. Admin Console → Resources → JDBC → Data sources
2. Cerca `jdbc/ds_sesamo` o `jdbc/nsd_entr`
3. Test connection

## 📚 Query di Esempio Aggiuntive

### INSERT con Auto-Detection
```sql
-- Inserimento in SESAMO
INSERT INTO sesamo.audit_log (log_id, action, action_date, user_name)
VALUES (audit_seq.NEXTVAL, 'Data Update', SYSDATE, USER);

-- Inserimento in ENTR_ASP
INSERT INTO entr_asp.user_sessions (session_id, login_time, ip_address)
VALUES (session_seq.NEXTVAL, SYSTIMESTAMP, '192.168.1.100');
```

### UPDATE con Condizioni Multiple
```sql
-- Aggiorna record in SESAMO
UPDATE sesamo.uffici
SET data_fine = SYSDATE,
    stato = 'ARCHIVIATO',
    last_modified_by = USER
WHERE id_ufficio IN (111, 112, 113)
  AND stato = 'ATTIVO';
```

### DELETE con Subquery
```sql
-- Elimina record obsoleti in SESAMO
DELETE FROM sesamo.temp_data
WHERE id_temp IN (
    SELECT id_temp
    FROM sesamo.temp_data
    WHERE created_date < ADD_MONTHS(SYSDATE, -6)
    AND status = 'PROCESSED'
);
```

### Operazioni Multiple (Stessa Transazione)

**Query 1:**
```sql
INSERT INTO sesamo.audit_log (log_id, action)
VALUES (1, 'Start Cleanup');
```
→ Execute → Vedi 1 row affected

**Query 2:**
```sql
DELETE FROM sesamo.old_records
WHERE archive_date < SYSDATE - 365;
```
→ Execute → Vedi N rows affected

**Query 3:**
```sql
INSERT INTO sesamo.audit_log (log_id, action)
VALUES (2, 'End Cleanup');
```
→ Execute → Vedi 1 row affected

**Poi:**
- Se tutto OK → **Commit** (tutte e 3 le operazioni vengono salvate)
- Se qualcosa è andato storto → **Rollback** (tutte e 3 le operazioni vengono annullate)

## 💡 Tips & Tricks

1. **Copia/Incolla da SQL Developer:**
   - Puoi copiare query direttamente da SQL Developer
   - Assicurati che abbiano lo schema qualifier (sesamo.xxx, entr_asp.yyy)

2. **Test Prima del Commit:**
   - Esegui una SELECT equivalente per verificare quali record saranno impattati:
     ```sql
     -- Prima esegui questo in SQL Developer per vedere cosa verrà modificato:
     SELECT * FROM sesamo.uffici WHERE id_ufficio = 111;

     -- Poi esegui l'UPDATE nell'applicazione:
     UPDATE sesamo.uffici SET data_fine = SYSDATE WHERE id_ufficio = 111;
     ```

3. **Batch Processing:**
   - Per operazioni massive, processa in batch di 1000-5000 record alla volta
   - Fai commit dopo ogni batch

4. **Logging:**
   - Tutte le operazioni sono loggate in `SystemOut.log`
   - Include session ID, query (primi 100 caratteri), e risultati

## 📞 Supporto

Se hai domande o problemi:
1. Controlla il file `README.md` per troubleshooting generale
2. Vedi `config/datasource-config.md` per problemi di connessione JNDI
3. Consulta i log: `${WAS_HOME}/profiles/AppSrv01/logs/server1/SystemOut.log`

---

**Happy Querying! 🚀**
