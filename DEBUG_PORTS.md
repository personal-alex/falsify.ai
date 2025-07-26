# Debug Port Configuration

This document explains the debug port assignments for running multiple services simultaneously without JDWP conflicts.

## Port Assignments

### HTTP Ports
- **crawler-caspit**: 8080
- **crawler-drucker**: 8081  
- **crawler-manager**: 8082
- **frontend**: 5173

### Debug Ports (JDWP)
- **crawler-caspit**: 5005
- **crawler-drucker**: 5006
- **crawler-manager**: 5007

## Available Scripts

### 1. `run-all-services.sh`
Starts all services including the frontend:
- crawler-manager (backend + frontend)
- crawler-caspit
- crawler-drucker

```bash
./run-all-services.sh
```

**Services:**
- Frontend: http://localhost:5173
- Manager API: http://localhost:8082
- Caspit Crawler: http://localhost:8080
- Drucker Crawler: http://localhost:8081

### 2. `run-crawlers-with-manager.sh`
Starts only backend services (no frontend):
- crawler-manager (backend only)
- crawler-caspit
- crawler-drucker

```bash
./run-crawlers-with-manager.sh
```

### 3. `run-both-crawlers.sh`
Starts only the crawler services:
- crawler-caspit
- crawler-drucker

```bash
./run-both-crawlers.sh
```

### 4. `crawler-manager/scripts/dev-start.sh`
Starts only the crawler-manager with frontend:
- crawler-manager (backend + frontend)

```bash
cd crawler-manager
./scripts/dev-start.sh
```

## IDE Debug Configuration

### IntelliJ IDEA / VS Code
Create separate debug configurations for each service:

**Caspit Crawler:**
- Host: localhost
- Port: 5005

**Drucker Crawler:**
- Host: localhost  
- Port: 5006

**Crawler Manager:**
- Host: localhost
- Port: 5007

## Manual Service Startup

If you prefer to start services individually:

```bash
# Terminal 1 - Caspit Crawler
mvn quarkus:dev -pl crawler-caspit -Ddebug=5005

# Terminal 2 - Drucker Crawler  
mvn quarkus:dev -pl crawler-drucker -Ddebug=5006

# Terminal 3 - Crawler Manager
mvn quarkus:dev -pl crawler-manager -Ddebug=5007

# Terminal 4 - Frontend (optional)
cd crawler-manager/frontend
npm run dev
```

## Troubleshooting

### Port Already in Use
If you get "Address already in use" errors:

1. **Check what's using the port:**
   ```bash
   lsof -i :5005  # or 5006, 5007
   ```

2. **Kill the process:**
   ```bash
   kill -9 <PID>
   ```

3. **Or kill all Java processes:**
   ```bash
   pkill -f "quarkus.*dev"
   ```

### Debug Connection Issues
- Ensure the service is fully started before connecting debugger
- Check that the correct debug port is being used
- Verify no firewall is blocking the debug ports

## Log Files

Each script creates separate log files:
- `manager.log` - Crawler Manager output
- `caspit.log` - Caspit Crawler output  
- `drucker.log` - Drucker Crawler output
- `frontend.log` - Frontend output (when applicable)

Monitor logs with:
```bash
# Watch all logs
tail -f *.log

# Watch specific service
tail -f caspit.log
```