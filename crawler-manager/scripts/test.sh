#!/bin/bash

# Test script for Crawler Manager
# Runs both backend and frontend tests

set -e

echo "🧪 Running Crawler Manager tests..."

# Run backend tests
echo "🔧 Running backend tests..."
mvn test

# Run frontend tests if node_modules exists
if [ -d "frontend/node_modules" ]; then
    echo "🎨 Running frontend tests..."
    cd frontend
    npm run test
    cd ..
else
    echo "⚠️  Frontend dependencies not installed. Run 'npm install' in frontend directory first."
fi

echo "✅ All tests completed!"