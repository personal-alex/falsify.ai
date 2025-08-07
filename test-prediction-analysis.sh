#!/bin/bash

# Test Prediction Analysis Module
# This script tests the prediction analysis endpoints

BASE_URL="http://localhost:8083/api/prediction-analysis"

echo "Testing Prediction Analysis Module..."
echo "Base URL: $BASE_URL"
echo ""

# Test 1: Check system status
echo "1. Testing system status..."
curl -s "$BASE_URL/status" | jq '.' || echo "Failed to get system status"
echo ""

# Test 2: Check extractor status
echo "2. Testing extractor status..."
curl -s "$BASE_URL/extractors/status" | jq '.' || echo "Failed to get extractor status"
echo ""

# Test 3: Test prediction extraction with sample text
echo "3. Testing prediction extraction..."
curl -s -X POST "$BASE_URL/test" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "The stock market will rise by 15% next year according to financial analysts. Experts predict that inflation will decrease to 2% by the end of 2024. The unemployment rate is expected to drop below 4% in the coming months.",
    "title": "Economic Predictions for 2024"
  }' | jq '.' || echo "Failed to test prediction extraction"
echo ""

# Test 4: Get configuration details
echo "4. Testing configuration details..."
curl -s "$BASE_URL/config" | jq '.' || echo "Failed to get configuration"
echo ""

# Test 5: Get job history
echo "5. Testing job history..."
curl -s "$BASE_URL/history" | jq '.' || echo "Failed to get job history"
echo ""

echo "Testing completed!"
echo ""
echo "If you see JSON responses above, the module is working correctly."
echo "If Gemini is properly configured, you should see 'gemini' as the extractor type."
echo "If Gemini is not available, it will fall back to 'mock' extractor."