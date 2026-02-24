#!/bin/bash

###############################################################################
# AWS EC2 Deployment Script
# Provisions and deploys LLM Conversational Agent on AWS EC2
# Author: Harsh Jain
# Usage: ./aws-ec2-deploy.sh <instance-id> <pem-key-path> [region]
###############################################################################

set -e

# Configuration
INSTANCE_ID=${1:-}
PEM_KEY=${2:-}
REGION=${3:-us-east-1}

if [ -z "$INSTANCE_ID" ] || [ -z "$PEM_KEY" ]; then
    echo "Usage: $0 <instance-id> <pem-key-path> [region]"
    echo ""
    echo "Example:"
    echo "  $0 i-0123456789abcdef0 /path/to/key.pem us-east-1"
    exit 1
fi

if [ ! -f "$PEM_KEY" ]; then
    echo "Error: PEM key not found at $PEM_KEY"
    exit 1
fi

# Get instance IP address
echo "Fetching EC2 instance details..."
INSTANCE_IP=$(aws ec2 describe-instances \
    --instance-ids "$INSTANCE_ID" \
    --region "$REGION" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text)

if [ -z "$INSTANCE_IP" ] || [ "$INSTANCE_IP" == "None" ]; then
    echo "Error: Could not retrieve IP for instance $INSTANCE_ID"
    exit 1
fi

echo "Instance IP: $INSTANCE_IP"

# Setup key permissions
chmod 400 "$PEM_KEY"

# Create remote deployment directory
echo "Setting up remote directories..."
ssh -i "$PEM_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" << 'EOF'
    mkdir -p /home/ec2-user/llm-agent
    cd /home/ec2-user/llm-agent
    echo "Remote directory ready"
EOF

# Copy project to instance (excluding build artifacts)
echo "Uploading project files..."
rsync -avz -e "ssh -i $PEM_KEY -o StrictHostKeyChecking=no" \
    --exclude 'target' \
    --exclude '.git' \
    --exclude '.idea' \
    --exclude '.bloop' \
    --exclude '*.jar' \
    . ec2-user@"$INSTANCE_IP":/home/ec2-user/llm-agent/

# Deploy on remote instance
echo "Installing dependencies and deploying..."
ssh -i "$PEM_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" << 'DEPLOY_SCRIPT'
    set -e
    
    echo "==================================="
    echo "EC2 Deployment: LLM Agent"
    echo "==================================="
    
    cd /home/ec2-user/llm-agent
    
    # Update system packages
    echo "1. Updating system packages..."
    sudo yum update -y
    
    # Install Docker
    echo "2. Installing Docker..."
    if ! command -v docker &> /dev/null; then
        sudo yum install -y docker
        sudo systemctl start docker
        sudo usermod -aG docker ec2-user
    fi
    
    # Install Docker Compose
    echo "3. Installing Docker Compose..."
    if ! command -v docker-compose &> /dev/null; then
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
    fi
    
    # Build and deploy
    echo "4. Building Docker image..."
    sudo docker-compose build
    
    echo "5. Starting services..."
    sudo docker-compose up -d
    
    echo "6. Waiting for services to be healthy..."
    sleep 30
    
    # Verify deployment
    echo "7. Verifying deployment..."
    if curl -s http://localhost:8080/health | grep -q "healthy"; then
        echo "✓ Service is running and healthy!"
    else
        echo "✗ Service health check failed"
        exit 1
    fi
    
    echo "==================================="
    echo "Deployment Complete!"
    echo "==================================="
    echo "API available at: http://$(hostname -I | awk '{print $1}'):8080"
    
DEPLOY_SCRIPT

# Display post-deployment info
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║               Deployment Successful!                          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Instance IP: $INSTANCE_IP"
echo "SSH Connection: ssh -i $PEM_KEY ec2-user@$INSTANCE_IP"
echo ""
echo "API Endpoints:"
echo "  Health: http://$INSTANCE_IP:8080/health"
echo "  Chat:   http://$INSTANCE_IP:8080/api/v1/chat"
echo "  Stats:  http://$INSTANCE_IP:8080/api/v1/stats"
echo ""
echo "View logs:"
echo "  ssh -i $PEM_KEY ec2-user@$INSTANCE_IP \"docker-compose logs -f llm-agent\""
echo ""
echo "Stop services:"
echo "  ssh -i $PEM_KEY ec2-user@$INSTANCE_IP \"cd ~/llm-agent && docker-compose down\""
echo ""
