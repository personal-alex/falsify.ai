#!/bin/bash

# Crawler Manager + Crawlers Development Setup
# Starts crawler-manager backend and both crawlers (no frontend)

echo "=========================================="
echo "Crawler Manager + Crawlers Setup"
echo "=========================================="
echo ""
echo "Starting backend services:"
echo "  • crawler-manager: http://localhost:8082 (debug: 5007)"
echo "  • crawler-caspit: http://localhost:8080 (debug: 5005)"
echo "  • crawler-drucker: http://localhost:8081 (debug: 5006)"
echo ""

# Function to cleanup background processes on exit
cleanup() {
    echo ""
    echo "🛑 Shutting down services..."
    
    if [ ! -z "$MANAGER_PID" ]; then
        kill $MANAGER_PID 2>/dev/null
        echo "  • Stopped crawler-manager (PID: $MANAGER_PID)"
    fi
    
    if [ ! -z "$CASPIT_PID" ]; then
        kill $CASPIT_PID 2>/dev/null
        echo "  • Stopped crawler-caspit (PID: $CASPIT_PID)"
    fi
    
    if [ ! -z "$DRUCKER_PID" ]; then
        kill $DRUCKER_PID 2>/dev/null
        echo "  • Stopped crawler-drucker (PID: $DRUCKER_PID)"
    fi
    
    # Wait for processes to terminate
    wait $MANAGER_PID $CASPIT_PID $DRUCKER_PID 2>/dev/null
    
    echo "All services stopped."
    echo "Log files preserved: manager.log, caspit.log, drucker.log"
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM EXIT

# Validate that all modules exist
for module in "crawler-manager" "crawler-caspit" "crawler-drucker"; do
    if [ ! -d "$module" ]; then
        echo "❌ Error: $module module not found"
        echo "   Make sure you're running this script from the project root directory"
        exit 1
    fi
done

# Check if required ports are available
check_port() {
    local port=$1
    local service=$2
    if lsof -i :$port >/dev/null 2>&1; then
        echo "❌ Error: Port $port is already in use (needed for $service)"
        echo "   Please stop the service using port $port and try again"
        exit 1
    fi
}

check_port 8080 "crawler-caspit"
check_port 8081 "crawler-drucker" 
check_port 8082 "crawler-manager"
check_port 5005 "crawler-caspit debug"
check_port 5006 "crawler-drucker debug"
check_port 5007 "crawler-manager debug"

echo "🚀 Starting backend services..."
echo ""

# Start crawler-caspit with debug port 5005
echo "🔧 Starting crawler-caspit on port 8080 (debug: 5005)..."
mvn quarkus:dev -pl crawler-caspit -Dquarkus.args="" -Ddebug=5005 > caspit.log 2>&1 &
CASPIT_PID=$!
echo "   PID: $CASPIT_PID"

# Wait for caspit to start
sleep 6

# Start crawler-drucker with debug port 5006
echo "🔧 Starting crawler-drucker on port 8081 (debug: 5006)..."
mvn quarkus:dev -pl crawler-drucker -Dquarkus.args="" -Ddebug=5006 > drucker.log 2>&1 &
DRUCKER_PID=$!
echo "   PID: $DRUCKER_PID"

# Wait for drucker to start
sleep 6

# Start crawler-manager with debug port 5007
echo "🔧 Starting crawler-manager on port 8082 (debug: 5007)..."
mvn quarkus:dev -pl crawler-manager -Dquarkus.args="--clean" -Ddebug=5007 > manager.log 2>&1 &
MANAGER_PID=$!
echo "   PID: $MANAGER_PID"

# Wait for manager to start
sleep 8

echo ""
echo "✅ All backend services are running!"
echo ""
echo "📍 Service Access Points:"
echo ""
echo "🎯 Crawler Manager:"
echo "   • API: http://localhost:8082"
echo "   • Dev UI: http://localhost:8082/q/dev/"
echo "   • Crawlers API: http://localhost:8082/api/crawlers"
echo "   • Debug Port: 5007"
echo ""
echo "🕷️  Crawler Services:"
echo "   • Caspit: http://localhost:8080 (debug: 5005)"
echo "   • Drucker: http://localhost:8081 (debug: 5006)"
echo ""
echo "📋 Log Files:"
echo "   • manager.log - crawler-manager output"
echo "   • caspit.log - crawler-caspit output"
echo "   • drucker.log - crawler-drucker output"
echo ""
echo "💡 Quick Test Commands:"
echo "   • List crawlers: curl http://localhost:8082/api/crawlers"
echo "   • Caspit health: curl http://localhost:8080/q/health"
echo "   • Drucker health: curl http://localhost:8081/q/health"
echo ""
echo "🔧 Debug Ports (for IDE connection):"
echo "   • Crawler Manager: 5007"
echo "   • Caspit Crawler: 5005"
echo "   • Drucker Crawler: 5006"
echo ""
echo "🛑 Press Ctrl+C to stop all services"
echo ""

# Monitor all processes
while true; do
    # Check if all processes are still running
    services_running=0
    
    if kill -0 $MANAGER_PID 2>/dev/null; then
        services_running=$((services_running + 1))
    else
        echo "❌ crawler-manager process has stopped unexpectedly"
        echo "   Check manager.log for error details"
        break
    fi
    
    if kill -0 $CASPIT_PID 2>/dev/null; then
        services_running=$((services_running + 1))
    else
        echo "❌ crawler-caspit process has stopped unexpectedly"
        echo "   Check caspit.log for error details"
        break
    fi
    
    if kill -0 $DRUCKER_PID 2>/dev/null; then
        services_running=$((services_running + 1))
    else
        echo "❌ crawler-drucker process has stopped unexpectedly"
        echo "   Check drucker.log for error details"
        break
    fi
    
    # If all 3 services are running, continue monitoring
    if [ $services_running -eq 3 ]; then
        sleep 10
    else
        break
    fi
done

# If we reach here, one or more processes have stopped
echo ""
echo "⚠️  One or more services have stopped. Cleaning up..."
cleanup