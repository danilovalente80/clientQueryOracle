# Multi-Database Transaction Example

## Funzionalità

Il sistema supporta l'esecuzione di query su **database diversi nella stessa transazione**. Tutte le connessioni vengono arruolate nella stessa `UserTransaction`, garantendo un commit o rollback atomico su tutti i database coinvolti.

## Esempio d'uso

### Scenario: Aggiornamento su SESAMO e INSERT su ENTR_ASP

```sql
UPDATE sesamo.parametri_sistema
SET valore_parametro='nuovo_valore'
WHERE nome_parametro='PARAMETRO_X';

INSERT INTO entr_asp.log_operazioni
(id_log, descrizione, data_operazione)
VALUES (seq_log.NEXTVAL, 'Parametro aggiornato', SYSDATE);
```

### Come funziona

1. **Auto-Detection degli Alias**: Il sistema rileva automaticamente che la prima query usa l'alias `sesamo` e la seconda `entr_asp`

2. **Apertura Connessioni Multiple**:
   - Apre una connessione a `jdbc/ds_sesamo`
   - Apre una connessione a `jdbc/nsd_entr`
   - Entrambe vengono arruolate nella stessa `UserTransaction`

3. **Esecuzione Query**:
   - La prima query viene eseguita sulla connessione SESAMO
   - La seconda query viene eseguita sulla connessione ENTR_ASP
   - Affected rows: "1 - 1 TOTALE: 2"

4. **Commit/Rollback Atomico**:
   - **COMMIT**: Entrambe le modifiche vengono salvate permanentemente
   - **ROLLBACK**: Entrambe le modifiche vengono annullate

### Output Esempio

```
✓ SUCCESS  •  Rows: 1 - 1 TOTALE: 2

Query executed successfully. 2 row(s) affected.
Transaction pending - please commit or rollback.
```

Dopo il commit:
```
✓ Transaction committed successfully on database(s): sesamo,entr_asp. 2 row(s) affected.
```

## Requisiti

1. **Schema-Qualified Names**: Ogni query deve specificare lo schema (alias) nel nome della tabella:
   - ✅ `UPDATE sesamo.table SET ...`
   - ✅ `INSERT INTO entr_asp.table VALUES ...`
   - ❌ `UPDATE table SET ...` (non funziona senza alias)

2. **JNDI Configuration**: Tutti gli alias devono essere mappati a datasource JNDI validi in `DatabaseAlias` enum

3. **WebSphere Transaction Manager**: Il sistema usa il transaction manager di WebSphere per coordinare le transazioni distribuite

## Vantaggi

- **Atomicità**: Commit/Rollback su tutti i database simultaneamente
- **Trasparenza**: Il sistema gestisce automaticamente le connessioni multiple
- **Tracciabilità**: Il log registra tutti i database coinvolti nella transazione
- **Sicurezza**: Impossibile committare un database e rollbackare l'altro

## Note Tecniche

- Tutte le connessioni sono gestite in un `Map<String, Connection>`
- La `UserTransaction` coordina tutte le connessioni (XA transaction)
- Le connessioni vengono chiuse automaticamente dopo commit/rollback
- Il logging registra tutti gli alias utilizzati separati da virgola
