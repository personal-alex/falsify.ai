#!/bin/bash

# Setup Test Environment for Prediction Analysis Module

echo "Setting up test environment for Prediction Analysis Module..."

# Set test Gemini API key (for configuration testing)
export GEMINI_API_KEY="test-key-for-debugging"

# Set other environment variables
export DB_USERNAME=crawler_user
export DB_PASSWORD=crawler_password
export DB_URL=jdbc:postgresql://localhost:5432/crawler_db
export REDIS_URL=redis://localhost:6379

echo "Environment variables set:"
echo "  GEMINI_API_KEY: ${GEMINI_API_KEY:+***set***}"
echo "  DB_URL: $DB_URL"
echo "  REDIS_URL: $REDIS_URL"
echo ""

echo "You can now run:"
echo "  ./start-prediction-analysis.sh"
echo "  ./test-prediction-analysis.sh"
echo ""

echo "To set a real Gemini API key, run:"
echo "  export GEMINI_API_KEY='your-real-api-key-here'"