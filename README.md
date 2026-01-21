# Oracle Query Client

Un'applicazione Java EE per eseguire query DML (INSERT, UPDATE, DELETE) su database Oracle 21c con gestione manuale delle transazioni (commit/rollback).

## 📋 Caratteristiche

- ✅ Esecuzione di query INSERT, UPDATE, DELETE su Oracle 21c
- ✅ Gestione manuale delle transazioni con pulsanti Commit e Rollback
- ✅ Visualizzazione del numero di record impattati prima del commit
- ✅ Supporto multi-database tramite alias (entr_asp, sesamo, production, ecc.)
- ✅ Mapping dinamico alias → JNDI datasource
- ✅ Interfaccia web moderna e user-friendly
- ✅ Gestione errori completa con dettagli SQL
- ✅ Compatibile con WebSphere Application Server 8.5
- ✅ Architettura Java EE 6 (EJB 3.1, Servlet 3.0)
- ✅ Stateful session bean per mantenere la transazione tra richieste

## 🏗️ Architettura

```
clientQueryOracle (EAR)
├── clientQueryOracle-ejb (EJB Module)
│   ├── DatabaseAlias (enum per mapping alias → JNDI)
│   ├── QueryExecutorService (EJB Stateful con BMT)
│   └── Model classes (QueryRequest, QueryResponse)
│
├── clientQueryOracle-web (WAR Module)
│   ├── QueryServlet (REST-like endpoints)
│   ├── HTML/CSS/JavaScript frontend
│   └── WebSphere descriptors
│
└── Application descriptors (application.xml, ecc.)
```

## 🔧 Tecnologie Utilizzate

- **Java**: 7+ (compatibile con WAS 8.5)
- **Java EE**: 6
- **EJB**: 3.1 (Stateful, Bean Managed Transactions)
- **Servlet**: 3.0
- **Oracle Database**: 21c
- **JDBC Driver**: Oracle JDBC (ojdbc8)
- **Application Server**: WebSphere 8.5
- **Build Tool**: Maven 3.x
- **Frontend**: HTML5, CSS3, Vanilla JavaScript

## 📦 Struttura del Progetto

```
clientQueryOracle/
├── pom.xml                                 # Parent POM
├── README.md                               # Questo file
├── config/
│   ├── datasource-config.md               # Guida configurazione JNDI
│   └── sample-queries.sql                 # Query di esempio
│
├── clientQueryOracle-ejb/
│   ├── pom.xml
│   └── src/main/java/com/oracle/client/
│       ├── config/
│       │   └── DatabaseAlias.java         # Enum alias → JNDI
│       ├── model/
│       │   ├── QueryRequest.java          # DTO richiesta
│       │   └── QueryResponse.java         # DTO risposta
│       └── service/
│           ├── QueryExecutorService.java  # Interface
│           └── QueryExecutorServiceBean.java # EJB Implementation
│
├── clientQueryOracle-web/
│   ├── pom.xml
│   └── src/main/webapp/
│       ├── index.html                     # Frontend principale
│       ├── css/styles.css                 # Stili
│       ├── js/app.js                      # Logica frontend
│       └── WEB-INF/
│           ├── web.xml                    # Web descriptor
│           └── ibm-web-bnd.xml           # WebSphere bindings
│
└── clientQueryOracle-ear/
    ├── pom.xml
    └── src/main/application/META-INF/
        ├── application.xml                # EAR descriptor
        └── ibm-application-bnd.xml       # WebSphere bindings
```

## 🚀 Quick Start

### Prerequisiti

1. **Java Development Kit (JDK)**: 1.7 o superiore
2. **Maven**: 3.x
3. **WebSphere Application Server**: 8.5 o superiore
4. **Oracle Database**: 21c (o 19c, 18c)
5. **Oracle JDBC Driver**: ojdbc8.jar

### Step 1: Clone e Build

```bash
# Clone del repository (se in Git)
git clone <repository-url>
cd clientQueryOracle

# Build del progetto
mvn clean package

# Il file EAR sarà generato in:
# clientQueryOracle-ear/target/clientQueryOracle-ear-1.0.0.ear
```

### Step 2: Configurazione WebSphere

#### 2.1 Installazione JDBC Driver

```bash
# Copia il driver Oracle nella libreria di WebSphere
cp ojdbc8.jar ${WAS_HOME}/lib/

# Riavvia WebSphere
${WAS_HOME}/bin/stopServer.sh server1
${WAS_HOME}/bin/startServer.sh server1
```

#### 2.2 Configurazione JDBC Provider

1. Accedi alla console amministrativa: `http://localhost:9060/ibm/console`
2. Naviga: **Resources → JDBC → JDBC Providers**
3. Clicca **New** e configura:
   - **Database type**: Oracle
   - **Provider type**: Oracle JDBC Driver
   - **Implementation type**: Connection pool data source
   - **Name**: Oracle JDBC Provider
   - **Classpath**: `${WAS_INSTALL_ROOT}/lib/ojdbc8.jar`

#### 2.3 Configurazione Data Sources

Configura i seguenti datasources secondo i tuoi alias:

| Alias | JNDI Name | Esempio URL |
|-------|-----------|-------------|
| entr_asp | jdbc/EntrAspDS | jdbc:oracle:thin:@//host:1521/entr_asp |
| sesamo | jdbc/SesamoDS | jdbc:oracle:thin:@//host:1521/sesamo |
| production | jdbc/ProductionDS | jdbc:oracle:thin:@//host:1521/prod |
| development | jdbc/DevelopmentDS | jdbc:oracle:thin:@//host:1521/dev |
| test | jdbc/TestDS | jdbc:oracle:thin:@//host:1521/test |

Per istruzioni dettagliate, consulta: [config/datasource-config.md](config/datasource-config.md)

### Step 3: Deployment

#### Via Console Amministrativa

1. Accedi alla console: `http://localhost:9060/ibm/console`
2. Naviga: **Applications → Application Types → WebSphere enterprise applications**
3. Clicca **Install**
4. Seleziona il file: `clientQueryOracle-ear/target/clientQueryOracle-ear-1.0.0.ear`
5. Segui il wizard mantenendo le impostazioni di default
6. Clicca **Finish** e **Save**
7. Seleziona l'applicazione e clicca **Start**

#### Via wsadmin (Command Line)

```bash
cd ${WAS_HOME}/bin

./wsadmin.sh -lang jython -c "AdminApp.install('/path/to/clientQueryOracle-ear-1.0.0.ear', '[-appname clientQueryOracle -contextroot /queryOracle]')"

./wsadmin.sh -lang jython -c "AdminConfig.save()"

./wsadmin.sh -lang jython -c "AdminControl.invoke(AdminControl.queryNames('type=ApplicationManager,*'), 'startApplication', 'clientQueryOracle')"
```

### Step 4: Accesso all'Applicazione

Apri il browser e vai a:

```
http://localhost:9080/queryOracle
```

O se hai configurato un virtual host diverso:

```
http://your-server:port/queryOracle
```

## 📖 Utilizzo

### 1. Seleziona Database

Scegli l'alias del database dal menu a tendina:
- `entr_asp` - Enterprise ASP Database
- `sesamo` - Sesamo Database
- `production` - Production Database
- ecc.

### 2. Inserisci Query

Inserisci la tua query SQL nella textarea. Sono supportate solo:
- `INSERT`
- `UPDATE`
- `DELETE`

Esempio:
```sql
UPDATE employees
SET salary = salary * 1.10
WHERE department_id = 60;
```

### 3. Esegui Query

Clicca su **Execute Query**. L'applicazione mostrerà:
- ✅ Stato (Success/Error)
- 📊 Numero di record impattati
- 💬 Messaggio descrittivo
- ⚠️ Eventuali errori

### 4. Gestione Transazione

Dopo l'esecuzione, la transazione rimane aperta. Scegli:

- **Commit Changes** (✓): Rende le modifiche permanenti nel database
- **Rollback Changes** (↩): Annulla tutte le modifiche

⚠️ **IMPORTANTE**: Verifica sempre il numero di record impattati prima di fare commit!

## 🔐 Sicurezza

### Considerazioni di Sicurezza

1. **Autenticazione**: L'applicazione può essere integrata con la sicurezza di WebSphere
   - Decommentare la sezione `<security-constraint>` in `web.xml`
   - Configurare realm e ruoli in WebSphere

2. **Autorizzazione Database**:
   - Usare utenti database con privilegi limitati
   - Concedere solo INSERT, UPDATE, DELETE sulle tabelle necessarie
   - NON concedere privilegi DDL (CREATE, DROP, ALTER)

3. **Audit**:
   - Tutte le operazioni sono loggate in `SystemOut.log`
   - Includono session ID, query (prime 100 caratteri), e risultati

4. **SQL Injection**:
   - L'applicazione usa PreparedStatement
   - Tuttavia, validare sempre le query prima dell'esecuzione

5. **Network**:
   - Usare SSL/TLS per connessioni database in produzione
   - Configurare firewall per limitare accesso all'applicazione

### Best Practices

- ✅ Testare sempre su ambiente di sviluppo/test prima
- ✅ Eseguire SELECT prima di UPDATE/DELETE per verificare i record coinvolti
- ✅ Fare commit di piccoli batch per operazioni massive
- ✅ Documentare il motivo di ogni modifica
- ✅ Rivedere sempre il numero di record impattati prima del commit
- ❌ NON eseguire UPDATE/DELETE senza WHERE clause
- ❌ NON condividere credenziali database
- ❌ NON eseguire DDL statements

## 🛠️ Configurazione Avanzata

### Modifica degli Alias

Per aggiungere/modificare alias, edita:

**File**: `clientQueryOracle-ejb/src/main/java/com/oracle/client/config/DatabaseAlias.java`

```java
public enum DatabaseAlias {
    // Aggiungi il tuo alias qui
    NUOVO_ALIAS("nuovo_alias", "jdbc/NuovoAliasDS"),

    // Altri alias esistenti...
    ENTR_ASP("entr_asp", "jdbc/EntrAspDS"),
    SESAMO("sesamo", "jdbc/SesamoDS");

    // ... resto del codice
}
```

Dopo la modifica:
1. Rebuild: `mvn clean package`
2. Configura il datasource corrispondente in WebSphere
3. Redeploy l'applicazione

### Connection Pool Tuning

Per ottimizzare le performance:

1. **Min Connections**: 5-10 per ambiente con carico medio
2. **Max Connections**: 50-100 (basato su carico)
3. **Connection Timeout**: 180 secondi
4. **Statement Cache Size**: 50-100

Configurare in: **WebSphere Console → Resources → JDBC → Data sources → [DataSource] → Connection pool properties**

### Logging Avanzato

Per aumentare il livello di logging:

```bash
# Edita logging.properties di WAS o usa la console
# Per questa applicazione:
com.oracle.client.level=FINE
```

Logs disponibili in:
```
${WAS_HOME}/profiles/AppSrv01/logs/server1/SystemOut.log
```

## 🧪 Testing

### Test Manuali

1. **Test Connessione Database**:
   ```sql
   SELECT 1 FROM DUAL;
   ```
   (Usando SQL tool, non questa app)

2. **Test INSERT**:
   ```sql
   INSERT INTO test_table (id, name) VALUES (1, 'Test');
   ```

3. **Test UPDATE**:
   ```sql
   UPDATE test_table SET name = 'Updated' WHERE id = 1;
   ```

4. **Test DELETE**:
   ```sql
   DELETE FROM test_table WHERE id = 1;
   ```

5. **Test Rollback**:
   - Esegui un UPDATE
   - Verifica record impattati
   - Clicca Rollback
   - Verifica che i dati non siano cambiati

### Query di Test

Consulta [config/sample-queries.sql](config/sample-queries.sql) per esempi completi.

## 🐛 Troubleshooting

### Problema: "Unknown database alias"

**Soluzione**:
- Verifica che l'alias sia configurato in `DatabaseAlias.java`
- Controlla maiuscole/minuscole
- Rebuild e redeploy

### Problema: "Cannot get connection from datasource"

**Soluzione**:
1. Verifica che il datasource JNDI sia configurato in WebSphere
2. Test connection dal WebSphere Admin Console
3. Verifica credenziali database
4. Check che Oracle listener sia attivo: `lsnrctl status`

### Problema: "ClassNotFoundException: oracle.jdbc.driver.OracleDriver"

**Soluzione**:
- Copia ojdbc8.jar in `${WAS_HOME}/lib/`
- Riavvia WebSphere
- Verifica classpath in JDBC Provider configuration

### Problema: "Only INSERT, UPDATE, and DELETE queries are allowed"

**Soluzione**:
- L'applicazione blocca SELECT e DDL per sicurezza
- Se necessario, modificare validazione in `QueryExecutorServiceBean.java`

### Problema: "Transaction rolled back automatically"

**Cause**:
- Timeout transazione (default: 120 secondi)
- Perdita connessione database
- Session timeout

**Soluzione**:
- Aumentare transaction timeout in WebSphere
- Eseguire operazioni più piccole
- Verificare stabilità rete

### Log Files da Controllare

```bash
# WebSphere System Out
tail -f ${WAS_HOME}/profiles/AppSrv01/logs/server1/SystemOut.log

# WebSphere System Error
tail -f ${WAS_HOME}/profiles/AppSrv01/logs/server1/SystemErr.log

# Oracle Alert Log
tail -f ${ORACLE_HOME}/diag/rdbms/orcl/orcl/trace/alert_orcl.log
```

## 📊 Monitoring

### Metriche da Monitorare

1. **Connection Pool Usage**: WebSphere PMI
2. **Transaction Duration**: SystemOut.log
3. **Failed Queries**: SystemErr.log
4. **Active Sessions**: WebSphere Session monitoring

### Comandi Utili

```bash
# Check application status
${WAS_HOME}/bin/wsadmin.sh -c "print AdminControl.queryNames('type=Application,name=clientQueryOracle,*')"

# View JNDI bindings
${WAS_HOME}/bin/dumpNameSpace.sh

# Thread dump (se applicazione bloccata)
kill -3 <was_pid>
```

## 🔄 Upgrade e Manutenzione

### Aggiornamento Applicazione

```bash
# 1. Build nuova versione
mvn clean package

# 2. Stop applicazione
${WAS_HOME}/bin/wsadmin.sh -c "AdminControl.invoke(..., 'stopApplication', 'clientQueryOracle')"

# 3. Update tramite console o:
${WAS_HOME}/bin/wsadmin.sh -c "AdminApp.update('clientQueryOracle', 'app', '[-operation update -contents /path/to/new.ear]')"

# 4. Start applicazione
${WAS_HOME}/bin/wsadmin.sh -c "AdminControl.invoke(..., 'startApplication', 'clientQueryOracle')"
```

### Backup Configurazione

```bash
# Export configuration
${WAS_HOME}/bin/wsadmin.sh -c "AdminConfig.exportWasproduct('/backup/config.xml')"

# Backup application
cp clientQueryOracle-ear-1.0.0.ear /backup/
```

## 📝 Sviluppo

### Build Locale

```bash
# Compile
mvn clean compile

# Package
mvn clean package

# Install to local repo
mvn clean install

# Skip tests
mvn clean package -DskipTests

# Build singolo modulo
cd clientQueryOracle-ejb
mvn clean package
```

### IDE Setup

#### Eclipse/RAD

1. Importa come "Existing Maven Project"
2. Configura target runtime: WebSphere 8.5
3. Add EAR to server

#### IntelliJ IDEA

1. Open come Maven project
2. Configure Application Server: WebSphere 8.5
3. Setup artifact deployment

## 🤝 Contributi

Per contribuire al progetto:

1. Crea un branch per la feature: `git checkout -b feature/my-feature`
2. Commit changes: `git commit -am 'Add new feature'`
3. Push al branch: `git push origin feature/my-feature`
4. Crea Pull Request

## 📜 Licenza

[Specificare la licenza del progetto]

## 📞 Supporto

Per supporto e domande:
- **Email**: [email di supporto]
- **Issue Tracker**: [URL issue tracker]
- **Wiki**: [URL documentazione]

## 🙏 Crediti

Sviluppato per l'esecuzione di query DML su Oracle 21c con WebSphere Application Server 8.5.

---

**Versione**: 1.0.0
**Data**: Gennaio 2026
**Compatibilità**: WAS 8.5+, Oracle 21c, Java 7+
