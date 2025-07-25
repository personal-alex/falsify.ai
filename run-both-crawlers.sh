#!/bin/bash

# Multi-Crawler Startup Script
# Starts both crawler-caspit and crawler-drucker simultaneously on different ports

echo "=========================================="
echo "Multi-Crawler Development Setup"
echo "=========================================="
echo ""
echo "Starting both crawler modules simultaneously:"
echo "  • crawler-caspit: http://localhost:8080"
echo "  • crawler-drucker: http://localhost:8081"
echo ""

# Function to cleanup background processes on exit
cleanup() {
    echo ""
    echo "Stopping crawlers..."
    if [ ! -z "$CASPIT_PID" ]; then
        kill $CASPIT_PID 2>/dev/null
        echo "  • Stopped crawler-caspit (PID: $CASPIT_PID)"
    fi
    if [ ! -z "$DRUCKER_PID" ]; then
        kill $DRUCKER_PID 2>/dev/null
        echo "  • Stopped crawler-drucker (PID: $DRUCKER_PID)"
    fi
    
    # Wait for processes to terminate
    wait $CASPIT_PID $DRUCKER_PID 2>/dev/null
    
    echo "All crawlers stopped."
    echo "Log files preserved: caspit.log, drucker.log"
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM EXIT

# Validate that both modules exist
if [ ! -d "crawler-caspit" ]; then
    echo "❌ Error: crawler-caspit module not found"
    echo "   Make sure you're running this script from the project root directory"
    exit 1
fi

if [ ! -d "crawler-drucker" ]; then
    echo "❌ Error: crawler-drucker module not found"
    echo "   Make sure you're running this script from the project root directory"
    exit 1
fi

# Check if ports are available
if lsof -i :8080 >/dev/null 2>&1; then
    echo "❌ Error: Port 8080 is already in use"
    echo "   Please stop the service using port 8080 and try again"
    exit 1
fi

if lsof -i :8081 >/dev/null 2>&1; then
    echo "❌ Error: Port 8081 is already in use"
    echo "   Please stop the service using port 8081 and try again"
    exit 1
fi

# Start crawler-caspit in background
echo "🚀 Starting crawler-caspit on port 8080..."
mvn quarkus:dev -pl crawler-caspit -Dquarkus.args="" > caspit.log 2>&1 &
CASPIT_PID=$!

# Wait a bit for first crawler to start
echo "   Waiting for crawler-caspit to initialize..."
sleep 8

# Start crawler-drucker in background
echo "🚀 Starting crawler-drucker on port 8081..."
mvn quarkus:dev -pl crawler-drucker -Dquarkus.args="" > drucker.log 2>&1 &
DRUCKER_PID=$!

echo "   Waiting for crawler-drucker to initialize..."
sleep 8

echo ""
echo "✅ Both crawlers are starting up!"
echo ""
echo "📍 Access Points:"
echo "   • crawler-caspit: http://localhost:8080"
echo "     - Health: http://localhost:8080/q/health"
echo "     - Start crawl: curl -X POST http://localhost:8080/caspit/crawl"
echo "     - Status: curl http://localhost:8080/caspit/status"
echo ""
echo "   • crawler-drucker: http://localhost:8081"
echo "     - Health: http://localhost:8081/q/health"
echo "     - Start crawl: curl -X POST http://localhost:8081/drucker/crawl"
echo "     - Status: curl http://localhost:8081/drucker/status"
echo ""
echo "📋 Log Files:"
echo "   • caspit.log - crawler-caspit output"
echo "   • drucker.log - crawler-drucker output"
echo ""
echo "💡 Monitoring Commands:"
echo "   • Watch caspit logs: tail -f caspit.log"
echo "   • Watch drucker logs: tail -f drucker.log"
echo "   • Watch both logs: tail -f caspit.log drucker.log"
echo ""
echo "🛑 Press Ctrl+C to stop both crawlers"
echo ""

# Wait for both processes and monitor their status
while true; do
    # Check if both processes are still running
    if ! kill -0 $CASPIT_PID 2>/dev/null; then
        echo "❌ crawler-caspit process has stopped unexpectedly"
        echo "   Check caspit.log for error details"
        break
    fi
    
    if ! kill -0 $DRUCKER_PID 2>/dev/null; then
        echo "❌ crawler-drucker process has stopped unexpectedly"
        echo "   Check drucker.log for error details"
        break
    fi
    
    # Sleep for a bit before checking again
    sleep 5
done

# If we reach here, one of the processes has stopped
echo ""
echo "⚠️  One or more crawlers have stopped. Cleaning up..."
cleanup