#!/bin/bash

# Multi-Crawler Setup Testing Script
# Tests simultaneous crawler operation and verifies all requirements

echo "=========================================="
echo "Multi-Crawler Setup Testing"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_result="$3"
    
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    echo -n "🧪 Testing: $test_name... "
    
    if eval "$test_command" >/dev/null 2>&1; then
        if [ "$expected_result" = "success" ]; then
            echo -e "${GREEN}✅ PASSED${NC}"
            TESTS_PASSED=$((TESTS_PASSED + 1))
            return 0
        else
            echo -e "${RED}❌ FAILED (expected failure but got success)${NC}"
            TESTS_FAILED=$((TESTS_FAILED + 1))
            return 1
        fi
    else
        if [ "$expected_result" = "failure" ]; then
            echo -e "${GREEN}✅ PASSED (expected failure)${NC}"
            TESTS_PASSED=$((TESTS_PASSED + 1))
            return 0
        else
            echo -e "${RED}❌ FAILED${NC}"
            TESTS_FAILED=$((TESTS_FAILED + 1))
            return 1
        fi
    fi
}

# Function to test HTTP endpoint
test_http_endpoint() {
    local url="$1"
    local expected_status="$2"
    local timeout="${3:-5}"
    
    local actual_status=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout $timeout "$url" 2>/dev/null)
    [ "$actual_status" = "$expected_status" ]
}

# Function to test JSON response
test_json_response() {
    local url="$1"
    local timeout="${2:-5}"
    
    local response=$(curl -s --connect-timeout $timeout "$url" 2>/dev/null)
    echo "$response" | jq . >/dev/null 2>&1
}

echo "Phase 1: Configuration Validation"
echo "=================================="

# Test 1: Verify project structure
run_test "Project structure exists" "[ -d 'crawler-caspit' ] && [ -d 'crawler-drucker' ] && [ -d 'crawler-common' ]" "success"

# Test 2: Verify configuration files
run_test "Configuration files exist" "[ -f 'crawler-caspit/src/main/resources/application.properties' ] && [ -f 'crawler-drucker/src/main/resources/application.properties' ]" "success"

# Test 3: Verify port configurations
echo -n "🧪 Testing: Port configurations... "
CASPIT_PORT=$(grep "quarkus.http.port" crawler-caspit/src/main/resources/application.properties | cut -d'=' -f2)
DRUCKER_PORT=$(grep "quarkus.http.port" crawler-drucker/src/main/resources/application.properties | cut -d'=' -f2)

if [ "$CASPIT_PORT" = "8080" ] && [ "$DRUCKER_PORT" = "8081" ]; then
    echo -e "${GREEN}✅ PASSED${NC} (caspit:$CASPIT_PORT, drucker:$DRUCKER_PORT)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ FAILED${NC} (caspit:$CASPIT_PORT, drucker:$DRUCKER_PORT)"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
TESTS_TOTAL=$((TESTS_TOTAL + 1))

# Test 4: Verify Redis key prefix configurations
echo -n "🧪 Testing: Redis key prefix configurations... "
CASPIT_PREFIX=$(grep "crawler.common.redis.key-prefix" crawler-caspit/src/main/resources/application.properties | cut -d'=' -f2)
DRUCKER_PREFIX=$(grep "crawler.common.redis.key-prefix" crawler-drucker/src/main/resources/application.properties | cut -d'=' -f2)

if [ "$CASPIT_PREFIX" = "crawler:caspit" ] && [ "$DRUCKER_PREFIX" = "crawler:drucker" ]; then
    echo -e "${GREEN}✅ PASSED${NC} (caspit:$CASPIT_PREFIX, drucker:$DRUCKER_PREFIX)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ FAILED${NC} (caspit:$CASPIT_PREFIX, drucker:$DRUCKER_PREFIX)"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
TESTS_TOTAL=$((TESTS_TOTAL + 1))

# Test 5: Verify crawler source configurations
echo -n "🧪 Testing: Crawler source configurations... "
CASPIT_SOURCE=$(grep "caspit.crawler.source" crawler-caspit/src/main/resources/application.properties | cut -d'=' -f2)
DRUCKER_SOURCE=$(grep "crawler.source.name" crawler-drucker/src/main/resources/application.properties | cut -d'=' -f2)

if [ "$CASPIT_SOURCE" = "caspit" ] && [ "$DRUCKER_SOURCE" = "drucker" ]; then
    echo -e "${GREEN}✅ PASSED${NC} (caspit:$CASPIT_SOURCE, drucker:$DRUCKER_SOURCE)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ FAILED${NC} (caspit:$CASPIT_SOURCE, drucker:$DRUCKER_SOURCE)"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
TESTS_TOTAL=$((TESTS_TOTAL + 1))

echo ""
echo "Phase 2: Compilation Testing"
echo "============================="

# Test 6: Compile crawler-caspit
run_test "crawler-caspit compilation" "mvn compile -pl crawler-caspit -q" "success"

# Test 7: Compile crawler-drucker
run_test "crawler-drucker compilation" "mvn compile -pl crawler-drucker -q" "success"

# Test 8: Compile crawler-common
run_test "crawler-common compilation" "mvn compile -pl crawler-common -q" "success"

echo ""
echo "Phase 3: Port Availability Testing"
echo "==================================="

# Test 9: Check port 8080 availability
echo -n "🧪 Testing: Port 8080 availability... "
if lsof -i :8080 >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  WARNING${NC} (port in use - may affect testing)"
else
    echo -e "${GREEN}✅ PASSED${NC} (port available)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
fi
TESTS_TOTAL=$((TESTS_TOTAL + 1))

# Test 10: Check port 8081 availability
echo -n "🧪 Testing: Port 8081 availability... "
if lsof -i :8081 >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  WARNING${NC} (port in use - may affect testing)"
else
    echo -e "${GREEN}✅ PASSED${NC} (port available)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
fi
TESTS_TOTAL=$((TESTS_TOTAL + 1))

echo ""
echo "Phase 4: Script Validation"
echo "=========================="

# Test 11: Verify startup script exists and is executable
run_test "Startup script exists and is executable" "[ -f 'run-both-crawlers.sh' ] && [ -x 'run-both-crawlers.sh' ]" "success"

echo ""
echo "Phase 5: Live Testing (Optional)"
echo "================================"

echo -e "${BLUE}ℹ️  Live testing requires starting the crawlers${NC}"
echo "   Run the following commands to test live functionality:"
echo ""
echo "   1. Start both crawlers:"
echo "      ./run-both-crawlers.sh"
echo ""
echo "   2. In another terminal, test the endpoints:"
echo "      # Test caspit health"
echo "      curl http://localhost:8080/q/health"
echo ""
echo "      # Test drucker health"
echo "      curl http://localhost:8081/q/health"
echo ""
echo "      # Test caspit status"
echo "      curl http://localhost:8080/caspit/status"
echo ""
echo "      # Test drucker status"
echo "      curl http://localhost:8081/drucker/status"
echo ""
echo "      # Start caspit crawl"
echo "      curl -X POST http://localhost:8080/caspit/crawl"
echo ""
echo "      # Start drucker crawl"
echo "      curl -X POST http://localhost:8081/drucker/crawl"
echo ""

# Check if crawlers are already running for live testing
if curl -s http://localhost:8080/q/health >/dev/null 2>&1 && curl -s http://localhost:8081/q/health >/dev/null 2>&1; then
    echo -e "${GREEN}🎉 Both crawlers are running! Running live tests...${NC}"
    echo ""
    
    # Test 12: Test caspit health endpoint
    run_test "crawler-caspit health endpoint" "test_http_endpoint 'http://localhost:8080/q/health' '200'" "success"
    
    # Test 13: Test drucker health endpoint
    run_test "crawler-drucker health endpoint" "test_http_endpoint 'http://localhost:8081/q/health' '200'" "success"
    
    # Test 14: Test caspit status endpoint
    run_test "crawler-caspit status endpoint" "test_http_endpoint 'http://localhost:8080/caspit/status' '200'" "success"
    
    # Test 15: Test drucker status endpoint
    run_test "crawler-drucker status endpoint" "test_http_endpoint 'http://localhost:8081/drucker/status' '200'" "success"
    
    # Test 16: Test caspit health returns valid JSON
    run_test "crawler-caspit health JSON response" "test_json_response 'http://localhost:8080/q/health'" "success"
    
    # Test 17: Test drucker health returns valid JSON
    run_test "crawler-drucker health JSON response" "test_json_response 'http://localhost:8081/q/health'" "success"
    
else
    echo -e "${YELLOW}⚠️  Crawlers not running - skipping live tests${NC}"
    echo "   Start crawlers with: ./run-both-crawlers.sh"
fi

echo ""
echo "=========================================="
echo "Test Results Summary"
echo "=========================================="
echo -e "Total Tests: ${BLUE}$TESTS_TOTAL${NC}"
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 All tests passed! Multi-crawler setup is ready.${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Start both crawlers: ./run-both-crawlers.sh"
    echo "2. Test crawling operations using the API endpoints"
    echo "3. Monitor logs: tail -f caspit.log drucker.log"
    exit 0
else
    echo ""
    echo -e "${RED}❌ Some tests failed. Please review the configuration.${NC}"
    echo ""
    echo "Common issues:"
    echo "• Port conflicts - check if other services are using ports 8080/8081"
    echo "• Configuration errors - verify application.properties files"
    echo "• Compilation errors - run 'mvn clean compile' to check for issues"
    exit 1
fi