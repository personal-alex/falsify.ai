#!/bin/bash

# Complete Multi-Service Development Setup
# Starts crawler-manager, crawler-caspit, and crawler-drucker simultaneously

echo "=========================================="
echo "Complete Multi-Service Development Setup"
echo "=========================================="
echo ""
echo "Starting all services simultaneously:"
echo "  • crawler-manager: http://localhost:8082 (debug: 5007)"
echo "  • crawler-caspit: http://localhost:8080 (debug: 5005)"
echo "  • crawler-drucker: http://localhost:8081 (debug: 5006)"
echo "  • frontend: http://localhost:5173"
echo ""

# Function to cleanup background processes on exit
cleanup() {
    echo ""
    echo "🛑 Shutting down all services..."
    
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
    
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null
        echo "  • Stopped frontend (PID: $FRONTEND_PID)"
    fi
    
    # Wait for processes to terminate
    wait $MANAGER_PID $CASPIT_PID $DRUCKER_PID $FRONTEND_PID 2>/dev/null
    
    echo "All services stopped."
    echo "Log files preserved: manager.log, caspit.log, drucker.log, frontend.log"
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

# Check if frontend directory exists
if [ ! -d "crawler-manager/frontend" ]; then
    echo "❌ Error: crawler-manager/frontend directory not found"
    exit 1
fi

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
check_port 5173 "frontend"

# Check if debug ports are available
check_port 5005 "crawler-caspit debug"
check_port 5006 "crawler-drucker debug"
check_port 5007 "crawler-manager debug"

# Check if Node.js and npm are installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18+ to continue."
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed. Please install npm to continue."
    exit 1
fi

# Install frontend dependencies if needed
if [ ! -d "crawler-manager/frontend/node_modules" ]; then
    echo "📦 Installing frontend dependencies..."
    cd crawler-manager/frontend
    npm install
    cd ../..
fi

echo "🚀 Starting all services..."
echo ""

# Start crawler-caspit with debug port 5005
echo "🔧 Starting crawler-caspit on port 8080 (debug: 5005)..."
mvn quarkus:dev -pl crawler-caspit -Dquarkus.args="" -Ddebug=5005 > caspit.log 2>&1 &
CASPIT_PID=$!
echo "   PID: $CASPIT_PID"

# Wait for caspit to start
sleep 15

# Start crawler-drucker with debug port 5006
echo "🔧 Starting crawler-drucker on port 8081 (debug: 5006)..."
mvn quarkus:dev -pl crawler-drucker -Dquarkus.args="" -Ddebug=5006 > drucker.log 2>&1 &
DRUCKER_PID=$!
echo "   PID: $DRUCKER_PID"

# Wait for drucker to start
sleep 15

# Start crawler-manager with debug port 5007
echo "🔧 Starting crawler-manager on port 8082 (debug: 5007)..."
mvn quarkus:dev -pl crawler-manager -Dquarkus.args="--clean" -Ddebug=5007 > manager.log 2>&1 &
MANAGER_PID=$!
echo "   PID: $MANAGER_PID"

# Wait for manager to start
sleep 8

# Start frontend development server
echo "🎨 Starting Vue.js frontend on port 5173..."
cd crawler-manager/frontend
npm run dev > ../../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ../..
echo "   PID: $FRONTEND_PID"

# Wait for frontend to start
sleep 4

echo ""
echo "✅ All services are starting up!"
echo ""
echo "📍 Service Access Points:"
echo ""
echo "🎯 Crawler Manager:"
echo "   • Frontend UI: http://localhost:5173"
echo "   • Backend API: http://localhost:8082"
echo "   • Dev UI: http://localhost:8082/q/dev/"
echo "   • Debug Port: 5007"
echo ""
echo "🕷️  Crawler Services:"
echo "   • Caspit Crawler: http://localhost:8080 (debug: 5005)"
echo "     - Health: http://localhost:8080/q/health"
echo "     - Start crawl: curl -X POST http://localhost:8080/caspit/crawl"
echo "     - Status: curl http://localhost:8080/caspit/status"
echo ""
echo "   • Drucker Crawler: http://localhost:8081 (debug: 5006)"
echo "     - Health: http://localhost:8081/q/health"
echo "     - Start crawl: curl -X POST http://localhost:8081/drucker/crawl"
echo "     - Status: curl http://localhost:8081/drucker/status"
echo ""
echo "📋 Log Files:"
echo "   • manager.log - crawler-manager output"
echo "   • caspit.log - crawler-caspit output"
echo "   • drucker.log - crawler-drucker output"
echo "   • frontend.log - Vue.js frontend output"
echo ""
echo "💡 Monitoring Commands:"
echo "   • Watch all logs: tail -f manager.log caspit.log drucker.log frontend.log"
echo "   • Watch specific service: tail -f <service>.log"
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
    
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        services_running=$((services_running + 1))
    else
        echo "❌ frontend process has stopped unexpectedly"
        echo "   Check frontend.log for error details"
        break
    fi
    
    # If all 4 services are running, continue monitoring
    if [ $services_running -eq 4 ]; then
        sleep 10
    else
        break
    fi
done

# If we reach here, one or more processes have stopped
echo ""
echo "⚠️  One or more services have stopped. Cleaning up..."
cleanup