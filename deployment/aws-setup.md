# AWS Deployment Guide: LLM Conversational Agent

This guide provides comprehensive instructions for deploying the LLM Conversational Agent on AWS using EC2, API Gateway, Lambda, and other AWS services.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [EC2 Setup](#ec2-setup)
3. [Docker Deployment](#docker-deployment)
4. [AWS API Gateway Setup](#aws-api-gateway-setup)
5. [Lambda Integration](#lambda-integration)
6. [IAM Configuration](#iam-configuration)
7. [Monitoring and Logging](#monitoring-and-logging)
8. [Cost Optimization](#cost-optimization)

## Prerequisites

- AWS account with administrative access
- AWS CLI configured with credentials
- Docker installed locally (for testing)
- Scala/SBT knowledge
- Basic AWS knowledge (EC2, IAM, API Gateway, Lambda)

## EC2 Setup

### 1. Launch an EC2 Instance

```bash
# Using AWS CLI to launch an instance
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.medium \
  --key-name your-key-pair \
  --security-groups your-security-group \
  --region us-east-1 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=llm-agent}]'
```

### 2. Instance Configuration

**Recommended Specifications:**
- **Instance Type**: t3.medium or t3.large
- **AMI**: Amazon Linux 2 or Ubuntu 22.04 LTS
- **Storage**: 30-50 GB EBS volume
- **Network**: VPC with public IP

### 3. Security Group Configuration

Create a security group with the following inbound rules:

| Protocol | Port | Source | Purpose |
|----------|------|--------|---------|
| SSH | 22 | Your IP | Management |
| HTTP | 80 | 0.0.0.0/0 | API Gateway |
| HTTPS | 443 | 0.0.0.0/0 | API Gateway |
| Custom | 8080 | 0.0.0.0/0 | Application |
| Custom | 11434 | VPC CIDR | Ollama (if local) |

## Docker Deployment

### 1. Install Docker and Docker Compose

```bash
# SSH into your EC2 instance
ssh -i your-key.pem ec2-user@your-instance-ip

# Update system packages
sudo yum update -y

# Install Docker
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. Clone and Deploy

```bash
# Clone the repository
git clone https://github.com/Harshjain5903/LLM-Conversational-Agent.git
cd LLM-Conversational-Agent

# Create necessary directories
mkdir -p /data/ollama
mkdir -p /var/log/llm-agent

# Start services with Docker Compose
docker-compose up -d

# Verify services are running
docker-compose ps
docker logs llm-agent
```

### 3. Configure Bedrock (Optional)

If using AWS Bedrock instead of Ollama:

```bash
# Update application.conf
sudo vi src/main/resources/application.conf

# Change backend to bedrock:
# app.llm.backend = "bedrock"

# Rebuild and restart
docker-compose down
docker-compose up -d --build
```

## AWS API Gateway Setup

### 1. Create API Gateway

```bash
# Using AWS CLI
aws apigateway create-rest-api \
  --name "LLM-Conversational-Agent-API" \
  --description "REST API for LLM Conversational Agent" \
  --endpoint-configuration types=REGIONAL
```

### 2. Configure Resources and Methods

**Create Resources:**
- POST /api/v1/chat
- GET /api/v1/stats
- GET /api/v1/conversation/{conversationId}
- GET /health

### 3. Integration: HTTP Backend

For each resource, configure an HTTP integration pointing to:
```
http://your-ec2-instance:8080
```

### 4. Deploy API

```bash
# Create deployment stage
aws apigateway create-deployment \
  --rest-api-id your-api-id \
  --stage-name prod \
  --stage-description "Production deployment"
```

## Lambda Integration

### 1. Create Lambda Function for Request Processing

```python
# lambda_function.py
import json
import urllib3
import os

http = urllib3.PoolManager()
AGENT_URL = os.environ['AGENT_URL']

def lambda_handler(event, context):
    """
    Lambda handler for conversational agent requests
    """
    try:
        # Extract message from API Gateway event
        body = json.loads(event.get('body', '{}'))
        message = body.get('message', '')
        conversation_id = body.get('conversationId', 'default')

        # Call the agent
        response = http.request(
            'POST',
            f'{AGENT_URL}/api/v1/chat',
            body=json.dumps({
                'message': message,
                'conversationId': conversation_id
            }),
            headers={'Content-Type': 'application/json'}
        )

        result = json.loads(response.data.decode('utf-8'))
        
        return {
            'statusCode': 200,
            'body': json.dumps(result),
            'headers': {'Content-Type': 'application/json'}
        }
    except Exception as e:
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)}),
            'headers': {'Content-Type': 'application/json'}
        }
```

### 2. Deploy Lambda

```bash
# Package Lambda function
zip lambda_function.zip lambda_function.py

# Create Lambda function
aws lambda create-function \
  --function-name llm-agent-handler \
  --runtime python3.11 \
  --role arn:aws:iam::YOUR_ACCOUNT_ID:role/lambda-execution-role \
  --handler lambda_function.lambda_handler \
  --zip-file fileb://lambda_function.zip \
  --environment Variables={AGENT_URL=http://your-ec2-instance:8080}
```

## IAM Configuration

### 1. EC2 Instance Role

Create an IAM role for EC2 instances to access Bedrock and CloudWatch:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:ListModels",
        "bedrock-runtime:InvokeModel"
      ],
      "Resource": "arn:aws:bedrock:*:*:foundation-model/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Resource": "*"
    }
  ]
}
```

### 2. Lambda Execution Role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface"
      ],
      "Resource": "*"
    }
  ]
}
```

## Monitoring and Logging

### 1. CloudWatch Logs

```bash
# Create log group
aws logs create-log-group --log-group-name /aws/llm-agent

# View logs
aws logs tail /aws/llm-agent --follow
```

### 2. CloudWatch Metrics

Configure application to send metrics:

```scala
// In your Scala application
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.{PutMetricDataRequest, MetricDatum}

val cloudwatch = CloudWatchClient.builder().build()

val metric = MetricDatum.builder()
  .metricName("RequestCount")
  .value(1.0)
  .build()

val request = PutMetricDataRequest.builder()
  .namespace("LLMAgent")
  .metricData(metric)
  .build()

cloudwatch.putMetricData(request)
```

### 3. Application Logs

Logs are written to `/var/log/llm-agent/application.log`

```bash
# View real-time logs
tail -f /var/log/llm-agent/application.log

# Search for errors
grep ERROR /var/log/llm-agent/application.log
```

## Cost Optimization

### 1. Reserved Instances

For production workloads, consider AWS EC2 Reserved Instances:
- **1-year commitment**: ~50% savings
- **3-year commitment**: ~70% savings

### 2. Auto Scaling

Configure Auto Scaling Group for multiple instances:

```bash
aws autoscaling create-auto-scaling-group \
  --auto-scaling-group-name llm-agent-asg \
  --launch-configuration-name llm-agent-lc \
  --min-size 1 \
  --max-size 5 \
  --desired-capacity 2 \
  --availability-zones us-east-1a us-east-1b
```

### 3. Using Spot Instances

```bash
# Launch as Spot instance for 70% discounts
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.medium \
  --instance-market-options "MarketType=spot"
```

### 4. Bedrock Pricing

- **On-demand**: Pay per output token
- **Provisioned throughput**: Fixed monthly cost for guaranteed capacity

Choose based on your traffic patterns.

### 5. Data Transfer Optimization

- Use VPC endpoints to minimize data transfer costs
- Cache responses appropriately
- Use CDN (CloudFront) for static content

## Troubleshooting

### Common Issues

**1. Container won't start**
```bash
docker logs llm-agent
# Check for port conflicts, insufficient memory, or configuration errors
```

**2. API Gateway timeout**
- Increase timeout settings in API Gateway
- Verify EC2 instance can handle the load
- Check security group rules

**3. Bedrock access denied**
```bash
# Verify IAM role has correct permissions
aws iam get-role --role-name your-role

# Add Bedrock permissions if needed
aws iam attach-role-policy \
  --role-name your-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonBedrockFullAccess
```

**4. High latency**
- Upgrade instance type (t3.large or c5.large)
- Enable request batching
- Consider caching responses
- Check network latency to LLM service

## Next Steps

1. **Custom Domain**: Configure Route 53 with your domain
2. **SSL/TLS**: Use AWS Certificate Manager for HTTPS
3. **CI/CD**: Set up CodePipeline for automated deployments
4. **Disaster Recovery**: Configure backup and failover procedures
5. **Load Testing**: Use Apache JMeter or Locust for testing

## Support

For more information, refer to:
- [AWS API Gateway Documentation](https://docs.aws.amazon.com/apigateway/)
- [AWS EC2 Documentation](https://docs.aws.amazon.com/ec2/)
- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)
