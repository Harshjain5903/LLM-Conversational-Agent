#!/bin/bash

###############################################################################
# LLM Conversational Agent - Interactive Demo
# Author: Harsh Jain
# Usage: ./demo.sh
###############################################################################

API_URL="http://localhost:8080"
CONV_ID="interactive-$(date +%s)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     LLM Conversational Agent - Interactive Demo               ║"
echo "║     Author: Harsh Jain                                        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if API is running
echo -e "${BLUE}Checking API connection...${NC}"
if ! curl -s "${API_URL}/health" > /dev/null 2>&1; then
    echo -e "${YELLOW}API is not running at ${API_URL}${NC}"
    echo -e "${YELLOW}Run 'docker-compose up --build' first${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Connected to API${NC}"
echo ""

echo -e "${CYAN}Welcome to the LLM Conversational Agent Demo!${NC}"
echo -e "${CYAN}Type 'quit' to exit, 'clear' to clear conversation${NC}"
echo -e "${CYAN}Session ID: ${CONV_ID}${NC}"
echo ""

message_count=0

while true; do
    echo -ne "${BLUE}You: ${NC}"
    read -r user_input
    
    # Check for commands
    if [ "$user_input" = "quit" ]; then
        echo -e "${GREEN}Goodbye!${NC}"
        break
    fi
    
    if [ "$user_input" = "clear" ]; then
        CONV_ID="interactive-$(date +%s)"
        message_count=0
        echo -e "${YELLOW}✓ Conversation cleared${NC}"
        continue
    fi
    
    if [ -z "$user_input" ]; then
        continue
    fi
    
    # Send message to API
    echo -ne "${YELLOW}Processing...${NC}\r"
    
    response=$(curl -s -X POST "${API_URL}/api/v1/chat" \
        -H "Content-Type: application/json" \
        -d "{\"message\":\"${user_input}\",\"conversationId\":\"${CONV_ID}\"}")
    
    # Extract response
    agent_response=$(echo "${response}" | jq -r '.response' 2>/dev/null)
    
    if [ -z "$agent_response" ] || [ "$agent_response" = "null" ]; then
        echo -e "${YELLOW}                        ${NC}"
        echo -e "${YELLOW}Assistant: Unable to get response${NC}"
        continue
    fi
    
    echo -e "${YELLOW}                        ${NC}"
    echo -e "${GREEN}Assistant: ${agent_response}${NC}"
    
    ((message_count++))
    echo ""
done

# Show statistics
echo ""
echo -e "${BLUE}Fetching conversation statistics...${NC}"
stats=$(curl -s -X GET "${API_URL}/api/v1/stats")
echo -e "${CYAN}${stats}${NC}"
