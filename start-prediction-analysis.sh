#!/bin/bash

# Start Prediction Analysis Module
# This script starts the prediction analysis module with proper environment variables

echo "Starting Prediction Analysis Module..."

# Set default environment variables if not already set
export DB_USERNAME=${DB_USERNAME:-crawler_user}
export DB_PASSWORD=${DB_PASSWORD:-crawler_password}
export DB_URL=${DB_URL:-jdbc:postgresql://localhost:5432/crawler_db}
export REDIS_URL=${REDIS_URL:-redis://localhost:6379}

# Set Gemini API key (you need to provide this)
if [ -z "$GEMINI_API_KEY" ]; then
    echo "WARNING: GEMINI_API_KEY environment variable is not set!"
    echo "Please set it with: export GEMINI_API_KEY='your-api-key'"
    echo "The module will fall back to mock predictions if Gemini is not available."
    echo ""
    echo "For testing purposes, you can set a dummy key:"
    echo "export GEMINI_API_KEY='test-key-for-debugging'"
    echo ""
fi

echo "Configuration:"
echo "  Database: $DB_URL"
echo "  Redis: $REDIS_URL"
echo "  Gemini API Key: ${GEMINI_API_KEY:+***set***}"
echo "  Port: 8083"
echo ""

# Start the module in development mode
cd prediction-analysis
mvn quarkus:dev

echo "Prediction Analysis Module stopped."