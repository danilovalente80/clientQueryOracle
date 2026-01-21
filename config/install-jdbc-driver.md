# Guida Rapida: Installazione Oracle JDBC Driver su WAS 8.5

## Step 1: Download Driver Oracle

1. Scarica **ojdbc8.jar** da Oracle:
   - https://www.oracle.com/database/technologies/jdbc-downloads.html
   - Oppure usa ojdbc7.jar per Java 7

## Step 2: Copia Driver nella Libreria WAS

### Windows:
```cmd
copy ojdbc8.jar C:\IBM\WebSphere\AppServer\lib\
```

### Linux/Mac:
```bash
cp ojdbc8.jar /opt/IBM/WebSphere/AppServer/lib/
```

## Step 3: Riavvia WebSphere

### Da Eclipse:
- Servers view → Click destro su WebSphere 8.5 → Restart

### Da Command Line:

**Windows:**
```cmd
cd C:\IBM\WebSphere\AppServer\bin
stopServer.bat server1
startServer.bat server1
```

**Linux/Mac:**
```bash
cd /opt/IBM/WebSphere/AppServer/bin
./stopServer.sh server1
./startServer.sh server1
```

## Step 4: Verifica

Controlla che il file sia presente:
```
${WAS_HOME}/lib/ojdbc8.jar
```
