# WebSphere DataSource Configuration for Oracle 21c

## Prerequisites

1. Oracle JDBC Driver (ojdbc8.jar or ojdbc11.jar for Oracle 21c)
2. WebSphere Application Server 8.5 or higher
3. Oracle Database 21c

## Step 1: Install Oracle JDBC Driver

1. Download Oracle JDBC driver from Oracle website:
   - For Java 8: `ojdbc8.jar`
   - Download from: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html

2. Copy the JDBC driver to WebSphere:
   ```bash
   cp ojdbc8.jar ${WAS_HOME}/lib/
   ```

3. Restart WebSphere after copying the driver.

## Step 2: Configure JDBC Providers

### Using WebSphere Admin Console:

1. Log in to WebSphere Admin Console (default: http://localhost:9060/ibm/console)

2. Navigate to: **Resources > JDBC > JDBC Providers**

3. Click **New** and configure:
   - **Database type**: Oracle
   - **Provider type**: Oracle JDBC Driver
   - **Implementation type**: Connection pool data source
   - **Name**: Oracle JDBC Provider
   - **Classpath**: `${WAS_INSTALL_ROOT}/lib/ojdbc8.jar`
   - **Implementation class**: `oracle.jdbc.pool.OracleConnectionPoolDataSource`

4. Click **OK** and **Save**

## Step 3: Configure Data Sources

Configure the following data sources according to your `DatabaseAlias` enum:

### 1. EntrAsp DataSource

1. Navigate to: **Resources > JDBC > Data sources**
2. Click **New** and configure:
   - **Data source name**: EntrAspDS
   - **JNDI name**: `jdbc/EntrAspDS`
   - **Select existing JDBC provider**: Oracle JDBC Provider
3. Click **Next** and configure connection properties:
   - **URL**: `jdbc:oracle:thin:@hostname:1521:SID` or `jdbc:oracle:thin:@//hostname:1521/ServiceName`
   - **Data store helper class**: `com.ibm.websphere.rsadapter.Oracle11gDataStoreHelper`
4. Configure authentication:
   - **Component-managed authentication alias**: Create new alias with database username/password
   - **Container-managed authentication alias**: Same as above
5. Test connection and **Save**

### 2. Sesamo DataSource

Repeat the above steps with:
- **Data source name**: SesamoDS
- **JNDI name**: `jdbc/SesamoDS`
- **URL**: Your Sesamo database URL
- **Authentication alias**: Sesamo credentials

### 3. Production DataSource

- **Data source name**: ProductionDS
- **JNDI name**: `jdbc/ProductionDS`
- **URL**: Your production database URL
- **Authentication alias**: Production credentials

### 4. Development DataSource

- **Data source name**: DevelopmentDS
- **JNDI name**: `jdbc/DevelopmentDS`
- **URL**: Your development database URL
- **Authentication alias**: Development credentials

### 5. Test DataSource

- **Data source name**: TestDS
- **JNDI name**: `jdbc/TestDS`
- **URL**: Your test database URL
- **Authentication alias**: Test credentials

## Step 4: Connection Pool Configuration

For each data source, configure connection pool settings:

1. Navigate to: **Resources > JDBC > Data sources > [Your DataSource] > Connection pool properties**

2. Recommended settings:
   - **Minimum connections**: 5
   - **Maximum connections**: 50
   - **Connection timeout**: 180 seconds
   - **Reap time**: 180 seconds
   - **Unused timeout**: 1800 seconds
   - **Aged timeout**: 0
   - **Purge policy**: Entire pool

## Step 5: Custom Properties (Optional)

Add custom properties for Oracle optimization:

1. Navigate to: **Resources > JDBC > Data sources > [Your DataSource] > Custom properties**

2. Add the following properties:
   - **oracle.net.CONNECT_TIMEOUT**: 10000
   - **oracle.jdbc.ReadTimeout**: 30000
   - **defaultRowPrefetch**: 20

## Step 6: Test Configuration

1. Navigate to each data source
2. Click **Test connection**
3. Verify connection is successful

## Step 7: Synchronize and Restart

1. If in a clustered environment:
   - Click **System administration > Nodes > Synchronize**
   - Select all nodes and click **Synchronize**

2. Restart the application server:
   ```bash
   ${WAS_HOME}/bin/stopServer.sh server1
   ${WAS_HOME}/bin/startServer.sh server1
   ```

## Example URLs for Oracle 21c

### Using SID:
```
jdbc:oracle:thin:@hostname:1521:ORCL
```

### Using Service Name (Recommended):
```
jdbc:oracle:thin:@//hostname:1521/service_name
```

### Using TNS:
```
jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=hostname)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=service_name)))
```

## Verification

After deployment, verify JNDI bindings:

1. Navigate to: **Environment > Naming > Name space bindings**
2. Search for `jdbc/` prefix
3. Verify all configured data sources are listed

## Troubleshooting

### Connection Issues:

1. Check Oracle listener is running:
   ```bash
   lsnrctl status
   ```

2. Verify network connectivity:
   ```bash
   telnet hostname 1521
   ```

3. Check WebSphere logs:
   ```
   ${WAS_HOME}/profiles/AppSrv01/logs/server1/SystemOut.log
   ```

### Common Errors:

- **ORA-12505**: Invalid SID - Use service name instead
- **ORA-28000**: Account locked - Unlock the database user
- **ClassNotFoundException**: JDBC driver not in classpath - Check driver location

## Security Considerations

1. Use separate database users for each environment
2. Grant only necessary privileges (INSERT, UPDATE, DELETE on specific tables)
3. Enable audit logging on Oracle side
4. Use SSL/TLS for production connections
5. Regularly rotate database passwords
6. Consider using WebSphere security domains for authentication
