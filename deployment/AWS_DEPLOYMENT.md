# AWS Deployment Guide - LLM Conversational Agent

This guide covers deploying the LLM Conversational Agent on AWS with full production infrastructure including EC2, API Gateway, Lambda, and monitoring.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Internet Users                              │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
                 ┌──────────────────────┐
                 │    AWS API Gateway   │
                 │   (HTTP Endpoint)    │
                 └──────────┬───────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   ┌─────────┐         ┌──────────┐      ┌──────────────┐
   │ Lambda  │         │ Lambda   │      │  VPC Link    │
   │ Chat    │         │ Stats    │      │   Route      │
   └────┬────┘         └────┬─────┘      └──────┬───────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                            ▼
                   ┌──────────────────┐
                   │   EC2 Instance   │
                   │  (Docker: Agent) │
                   └──────────────────┘
                            │
                ┌───────────┬┴──────────┐
                ▼           ▼          ▼
            ┌────────┐  ┌──────┐  ┌────────┐
            │ Ollama │  │ Logs │  │Metrics │
            └────────┘  └──────┘  └────────┘
```

## Prerequisites

- AWS Account with appropriate IAM permissions
- AWS CLI configured locally
- Terraform >= 1.0
- Docker and Docker Compose
- SSH access key for EC2
- (Optional) jq for JSON parsing

## Step 1: Create EC2 Instance

### Using AWS Console

1. Navigate to EC2 Dashboard
2. Click "Launch Instances"
3. **Select AMI**: Amazon Linux 2
4. **Instance Type**: t3.medium (recommended minimum)
5. **Storage**: 30GB EBS volume
6. **Security Group**: Allow ports 22, 8080, 3000, 9090, 9093
7. Create new key pair and download `.pem` file

### Using AWS CLI

```bash
# Create security group
aws ec2 create-security-group \
  --group-name llm-agent-sg \
  --description "Security group for LLM Agent" \
  --region us-east-1

# Add inbound rules
aws ec2 authorize-security-group-ingress \
  --group-name llm-agent-sg \
  --protocol tcp --port 22 --cidr 0.0.0.0/0 \
  --region us-east-1

aws ec2 authorize-security-group-ingress \
  --group-name llm-agent-sg \
  --protocol tcp --port 8080 --cidr 0.0.0.0/0 \
  --region us-east-1

# Launch instance
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.medium \
  --security-groups llm-agent-sg \
  --key-name my-key-pair \
  --region us-east-1
```

## Step 2: Deploy to EC2

### Quick Deployment

```bash
# Get your EC2 instance ID
INSTANCE_ID="i-0123456789abcdef0"
PEM_KEY="/path/to/your/key.pem"

# Run deployment script
./deployment/aws-ec2-deploy.sh $INSTANCE_ID $PEM_KEY us-east-1
```

The script will:
1. Install Docker and Docker Compose
2. Build the application image
3. Start services (Agent + Ollama)
4. Verify health checks
5. Return the instance public IP

### Manual Deployment

```bash
# SSH into instance
ssh -i your-key.pem ec2-user@<instance-ip>

# Clone repository
git clone https://github.com/Harshjain5903/LLM-Conversational-Agent.git
cd LLM-Conversational-Agent

# Build and run
docker-compose up --build -d

# Verify
curl http://localhost:8080/health
```

## Step 3: Configure API Gateway with Terraform

### Prerequisites

```bash
# Install Terraform
brew install terraform  # macOS
# or apt install terraform  # Linux

# Configure AWS credentials
aws configure
```

### Deployment

```bash
cd deployment

# Create terraform.tfvars
cat > terraform.tfvars << EOF
aws_region    = "us-east-1"
environment   = "prod"
project_name  = "llm-agent"
instance_url  = "http://<EC2-INSTANCE-IP>:8080"
EOF

# Initialize and deploy
terraform init
terraform plan
terraform apply

# Get outputs
terraform output api_endpoint
```

### API Gateway Routes

After deployment, your API Gateway will route:

- `POST /api/v1/chat` → Direct to EC2
- `GET /health` → Direct to EC2
- `GET /api/v1/stats` → Direct to EC2
- `GET /api/v1/conversation/{id}` → Direct to EC2
- `DELETE /api/v1/conversation/{id}` → Direct to EC2

## Step 4: Set Up Monitoring

### Start Monitoring Stack

```bash
# In deployment directory
cd deployment

# Start monitoring services
docker-compose -f monitoring-compose.yml up -d

# Verify services
curl http://localhost:9090  # Prometheus
curl http://localhost:3000  # Grafana (admin/admin123)
curl http://localhost:9093  # AlertManager
```

### Access Dashboards

| Service | URL | Credentials |
|---------|-----|-------------|
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin / admin123 |
| AlertManager | http://localhost:9093 | - |

### Configure Grafana

1. Login to Grafana (http://localhost:3000)
2. Add Prometheus data source: `http://prometheus:9090`
3. Import dashboard templates
4. Create custom dashboards for request latency, error rates, etc.

## Step 5: Enable CloudWatch Integration (Optional)

### IAM Role for EC2

```bash
# Create IAM role
aws iam create-role \
  --role-name llm-agent-ec2-role \
  --assume-role-policy-document file://trust-policy.json

# Attach CloudWatch policy
aws iam attach-role-policy \
  --role-name llm-agent-ec2-role \
  --policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy
```

### Send Metrics to CloudWatch

```bash
# Configure CloudWatch agent on EC2
ssh -i your-key.pem ec2-user@<instance-ip>

# Download agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm

# Install
sudo rpm -U ./amazon-cloudwatch-agent.rpm

# Configure and start
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a query -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/config.json -s
```

## Verification Checklist

```bash
# 1. Health check
curl http://<instance-ip>:8080/health
# Expected: {"status":"healthy","message":"..."}

# 2. API Gateway endpoint
curl https://<api-id>.execute-api.us-east-1.amazonaws.com/prod/health

# 3. Send message
curl -X POST http://<instance-ip>:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","conversationId":"test"}'

# 4. Prometheus scraping
curl http://localhost:9090/api/v1/targets

# 5. EC2 metrics in CloudWatch
aws cloudwatch get-metric-statistics \
  --namespace AWS/EC2 \
  --metric-name CPUUtilization \
  --start-time 2025-02-24T00:00:00Z \
  --end-time 2025-02-24T01:00:00Z \
  --period 60 \
  --statistics Average
```

## Monitoring Metrics

Key metrics to monitor:

### Application Metrics
- `llm_agent_request_duration_seconds` - Request latency (histogram)
- `llm_agent_requests_total` - Total requests (counter)
- `llm_agent_errors_total` - Total errors (counter)
- `llm_agent_active_conversations` - Active conversations (gauge)

### System Metrics
- CPU utilization
- Memory usage
- Disk I/O
- Network throughput

### AWS Metrics
- EC2 CPU, memory, disk
- API Gateway request count/latency
- Lambda invocations/errors/duration
- CloudWatch logs

## Troubleshooting

### EC2 Instance SSH Connection Failed
```bash
# Check security group allows port 22
aws ec2 describe-security-groups --group-names llm-agent-sg

# Verify instance is running
aws ec2 describe-instances --instance-ids i-xxxxx
```

### API Gateway 5XX Errors
```bash
# Check EC2 instance health
ssh into instance && curl http://localhost:8080/health

# Check VPC link status
aws apigatewayv2 get-integration --api-id <api-id> --integration-id <integration-id>
```

### Prometheus Not Scraping Metrics
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check agent metrics endpoint
curl http://localhost:8080/metrics
```

### High Memory Usage
```bash
# SSH into instance
docker stats llm-agent

# Increase JVM memory
docker-compose down
# Edit docker-compose.yml JAVA_OPTS
docker-compose up -d
```

## Cost Optimization

### Recommendations

1. **Instance Type**: Start with t3.small for dev, scale to t3.large for prod
2. **Auto Scaling**: Use EC2 Auto Scaling Groups with 2-3 instances
3. **Reserved Instances**: Purchase 1-year reservations for 40% savings
4. **Data Transfer**: Minimize cross-region traffic
5. **Lambda Tier**: Set reserved concurrency to avoid cold starts

### Cost Estimates (Monthly)

| Component | Size | Cost |
|-----------|------|------|
| EC2 (t3.medium) | 1 instance | $30 |
| API Gateway | 1M requests | $3.50 |
| Lambda | 1M invocations | $0.20 |
| Data Transfer | 10GB | $1 |
| **Total** | | **~$35** |

## Cleanup

### Remove all AWS resources

```bash
# Destroy Terraform resources
cd deployment
terraform destroy

# Terminate EC2 instance
aws ec2 terminate-instances --instance-ids i-xxxxx

# Delete security group
aws ec2 delete-security-group --group-name llm-agent-sg

# Delete IAM role and policies
aws iam detach-role-policy --role-name llm-agent-ec2-role \
  --policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy
aws iam delete-role --role-name llm-agent-ec2-role
```

## Support & Documentation

- [AWS EC2 Documentation](https://docs.aws.amazon.com/ec2/)
- [API Gateway Documentation](https://docs.aws.amazon.com/apigateway/)
- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest)
- [Prometheus Documentation](https://prometheus.io/docs/)

---

**Author**: Harsh Jain  
**Last Updated**: February 24, 2025  
**Project**: [LLM Conversational Agent](https://github.com/Harshjain5903/LLM-Conversational-Agent)
