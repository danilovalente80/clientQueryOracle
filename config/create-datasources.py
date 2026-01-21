#!/usr/bin/env jython
# Script Jython per creare automaticamente i datasources in WebSphere
#
# Utilizzo:
# wsadmin.sh -lang jython -f create-datasources.py
# oppure (Windows):
# wsadmin.bat -lang jython -f create-datasources.py

import sys

# ============================================
# CONFIGURAZIONE - MODIFICA QUESTI VALORI
# ============================================

# Informazioni Server
nodeName = 'localhostNode01'  # Usa AdminControl.getNode() per trovarlo
serverName = 'server1'
cellName = AdminControl.getCell()

# Informazioni Database Oracle
oracleHost = 'localhost'      # Hostname del database Oracle
oraclePort = '1521'           # Porta Oracle (default 1521)

# Credenziali Database (MODIFICA CON I TUOI VALORI)
dbUser = 'your_username'      # Username Oracle
dbPassword = 'your_password'  # Password Oracle

# JDBC Driver
jdbcDriverPath = '${WAS_INSTALL_ROOT}/lib/ojdbc8.jar'

# Datasources da creare (Alias → JNDI → Service Name)
datasources = [
    {
        'name': 'EntrAspDS',
        'jndi': 'jdbc/EntrAspDS',
        'serviceName': 'entr_asp',  # Service name / SID Oracle
        'user': dbUser,
        'password': dbPassword
    },
    {
        'name': 'SesamoDS',
        'jndi': 'jdbc/SesamoDS',
        'serviceName': 'sesamo',
        'user': dbUser,
        'password': dbPassword
    },
    {
        'name': 'ProductionDS',
        'jndi': 'jdbc/ProductionDS',
        'serviceName': 'prod',
        'user': dbUser,
        'password': dbPassword
    },
    {
        'name': 'DevelopmentDS',
        'jndi': 'jdbc/DevelopmentDS',
        'serviceName': 'dev',
        'user': dbUser,
        'password': dbPassword
    },
    {
        'name': 'TestDS',
        'jndi': 'jdbc/TestDS',
        'serviceName': 'test',
        'user': dbUser,
        'password': dbPassword
    }
]

# ============================================
# SCRIPT - NON MODIFICARE SOTTO QUESTA RIGA
# ============================================

print "=========================================="
print "Creating Oracle Datasources for WAS 8.5"
print "=========================================="
print ""

# Get server ID
serverId = AdminConfig.getid('/Node:' + nodeName + '/Server:' + serverName + '/')
if serverId == '':
    print "ERROR: Server not found: " + nodeName + "/" + serverName
    sys.exit(1)

print "Server ID: " + serverId
print ""

# Create JDBC Provider if not exists
print "Step 1: Creating JDBC Provider..."
providerName = 'Oracle JDBC Provider'
providers = AdminConfig.list('JDBCProvider', serverId).splitlines()

jdbcProvider = None
for provider in providers:
    if providerName in provider:
        jdbcProvider = provider
        print "  Found existing JDBC Provider: " + jdbcProvider
        break

if jdbcProvider == None:
    print "  Creating new JDBC Provider..."
    jdbcProvider = AdminConfig.create('JDBCProvider', serverId, [
        ['name', providerName],
        ['implementationClassName', 'oracle.jdbc.pool.OracleConnectionPoolDataSource'],
        ['classpath', jdbcDriverPath],
        ['description', 'Oracle JDBC Driver for Oracle 21c']
    ])
    print "  JDBC Provider created: " + jdbcProvider

print ""

# Create Datasources
print "Step 2: Creating Datasources..."
for ds in datasources:
    print ""
    print "  Creating datasource: " + ds['name']

    # Check if datasource already exists
    existingDS = AdminConfig.list('DataSource', jdbcProvider).splitlines()
    dsExists = False
    for existing in existingDS:
        if ds['name'] in existing:
            print "    WARNING: Datasource already exists, skipping: " + ds['name']
            dsExists = True
            break

    if dsExists:
        continue

    # Build JDBC URL
    jdbcUrl = 'jdbc:oracle:thin:@//' + oracleHost + ':' + oraclePort + '/' + ds['serviceName']
    print "    JDBC URL: " + jdbcUrl

    # Create DataSource
    newDS = AdminConfig.create('DataSource', jdbcProvider, [
        ['name', ds['name']],
        ['jndiName', ds['jndi']],
        ['description', 'Oracle datasource for ' + ds['serviceName']],
        ['authDataAlias', ''],  # Will be set later if needed
        ['statementCacheSize', 50],
        ['datasourceHelperClassname', 'com.ibm.websphere.rsadapter.Oracle11gDataStoreHelper']
    ])

    print "    Datasource created: " + newDS

    # Set connection pool properties
    connPool = AdminConfig.list('ConnectionPool', newDS)
    AdminConfig.modify(connPool, [
        ['maxConnections', 50],
        ['minConnections', 5],
        ['connectionTimeout', 180],
        ['reapTime', 180],
        ['agedTimeout', 1800]
    ])
    print "    Connection pool configured"

    # Set datasource properties (URL, user, password)
    propSet = AdminConfig.create('J2EEResourcePropertySet', newDS, [])

    # URL Property
    urlProp = AdminConfig.create('J2EEResourceProperty', propSet, [
        ['name', 'URL'],
        ['type', 'java.lang.String'],
        ['value', jdbcUrl]
    ])

    # User Property
    userProp = AdminConfig.create('J2EEResourceProperty', propSet, [
        ['name', 'user'],
        ['type', 'java.lang.String'],
        ['value', ds['user']]
    ])

    # Password Property
    passProp = AdminConfig.create('J2EEResourceProperty', propSet, [
        ['name', 'password'],
        ['type', 'java.lang.String'],
        ['value', ds['password']]
    ])

    print "    Properties configured (URL, user, password)"
    print "    JNDI Name: " + ds['jndi']
    print "    SUCCESS!"

print ""
print "=========================================="
print "Step 3: Saving configuration..."

# Save configuration
AdminConfig.save()
print "Configuration saved successfully!"

print ""
print "=========================================="
print "DATASOURCE CREATION COMPLETED"
print "=========================================="
print ""
print "Created datasources:"
for ds in datasources:
    print "  - " + ds['name'] + " -> " + ds['jndi']

print ""
print "Next steps:"
print "  1. Restart WebSphere Application Server"
print "  2. Test each datasource connection in Admin Console"
print "  3. Deploy clientQueryOracle application"
print ""
print "To test datasources:"
print "  Admin Console -> Resources -> JDBC -> Data sources"
print "  -> Select datasource -> Test connection"
print ""
