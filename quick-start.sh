#!/bin/bash

###############################################################################
# LLM Conversational Agent - Quick Start Script
# Author: Harsh Jain
# Usage: ./quick-start.sh
###############################################################################

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     LLM Conversational Agent - Quick Start                    ║"
echo "║     Author: Harsh Jain                                        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Docker
echo -e "${BLUE}1️⃣  Checking prerequisites...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}⚠️  Docker not found. Please install Docker from https://www.docker.com${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${YELLOW}⚠️  Docker Compose not found. Please install Docker Desktop${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker is installed${NC}"
echo ""

# Build and run
echo -e "${BLUE}2️⃣  Starting services...${NC}"
docker-compose up --build

echo ""
echo -e "${GREEN}✓ Services started successfully!${NC}"
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              API is running at http://localhost:8080           ║"
echo "║                                                                ║"
echo "║  Test the API with:                                           ║"
echo "║  curl http://localhost:8080/health                            ║"
echo "║                                                                ║"
echo "║  Send a message:                                              ║"
echo "║  curl -X POST http://localhost:8080/api/v1/chat \\            ║"
echo "║    -H \"Content-Type: application/json\" \\                     ║"
echo "║    -d '{\"message\":\"Hello\",\"conversationId\":\"user-1\"}'   ║"
echo "╚════════════════════════════════════════════════════════════════╝"
