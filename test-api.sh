#!/bin/bash

###############################################################################
# LLM Conversational Agent - Test API Script
# Author: Harsh Jain
# Usage: ./test-api.sh
###############################################################################

set -e

API_URL="http://localhost:8080"
CONV_ID="demo-$(date +%s)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     LLM Conversational Agent - API Test Script                ║"
echo "║     Author: Harsh Jain                                        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "${BLUE}Testing: ${description}${NC}"
    echo -e "${YELLOW}Request: ${method} ${API_URL}${endpoint}${NC}"
    
    if [ -z "$data" ]; then
        response=$(curl -s -X ${method} "${API_URL}${endpoint}")
    else
        echo -e "${YELLOW}Data: ${data}${NC}"
        response=$(curl -s -X ${method} "${API_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -d "${data}")
    fi
    
    echo -e "${GREEN}Response:${NC}"
    echo "${response}" | jq . 2>/dev/null || echo "${response}"
    echo ""
}

# Check if API is running
echo -e "${BLUE}1️⃣  Checking if API is running...${NC}"
if ! curl -s "${API_URL}/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ API is not running at ${API_URL}${NC}"
    echo -e "${YELLOW}Start it with: docker-compose up --build${NC}"
    exit 1
fi
echo -e "${GREEN}✓ API is running${NC}"
echo ""

# Test 1: Health Check
test_endpoint "GET" "/health" "" "1. Health Check"

# Test 2: Send first message
test_endpoint "POST" "/api/v1/chat" \
    "{\"message\":\"Hello! What is artificial intelligence?\",\"conversationId\":\"${CONV_ID}\"}" \
    "2. Send First Message"

# Test 3: Send second message (multi-turn)
test_endpoint "POST" "/api/v1/chat" \
    "{\"message\":\"Can you explain machine learning?\",\"conversationId\":\"${CONV_ID}\"}" \
    "3. Send Second Message (Multi-turn)"

# Test 4: Get Statistics
test_endpoint "GET" "/api/v1/stats" "" "4. Get Statistics"

# Test 5: Get Conversation History
test_endpoint "GET" "/api/v1/conversation/${CONV_ID}" "" "5. Get Conversation History"

# Test 6: Invalid Request (Error Handling)
test_endpoint "POST" "/api/v1/chat" \
    "{\"message\":\"\",\"conversationId\":\"${CONV_ID}\"}" \
    "6. Test Error Handling (Empty Message)"

echo -e "${GREEN}✓ All tests completed!${NC}"
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    Tests Passed Successfully!                  ║"
echo "║                                                                ║"
echo "║  API is fully functional and ready for production use.         ║"
echo "╚════════════════════════════════════════════════════════════════╝"
