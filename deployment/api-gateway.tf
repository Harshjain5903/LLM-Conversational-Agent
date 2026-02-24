# AWS API Gateway and Lambda Integration
# Terraform configuration for LLM Conversational Agent
# Author: Harsh Jain

terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# =====================================================
# Variables
# =====================================================

variable "aws_region" {
  description = "AWS region"
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  default     = "prod"
}

variable "project_name" {
  description = "Project name"
  default     = "llm-agent"
}

variable "instance_url" {
  description = "EC2 instance URL (e.g., http://10.0.0.100:8080)"
  type        = string
}

# =====================================================
# API Gateway
# =====================================================

resource "aws_apigatewayv2_api" "llm_agent_api" {
  name          = "${var.project_name}-api"
  protocol_type = "HTTP"
  
  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "DELETE", "OPTIONS"]
    allow_headers = ["*"]
  }

  tags = {
    Name        = "${var.project_name}-api"
    Environment = var.environment
  }
}

# =====================================================
# VPC Link (for connecting to EC2)
# =====================================================

resource "aws_apigatewayv2_vpc_link" "llm_agent_vpc_link" {
  name           = "${var.project_name}-vpc-link"
  security_group_ids = [aws_security_group.llm_agent_vpc_link.id]
  subnet_ids     = data.aws_subnets.default.ids

  tags = {
    Name        = "${var.project_name}-vpc-link"
    Environment = var.environment
  }
}

# =====================================================
# Security Group for VPC Link
# =====================================================

resource "aws_security_group" "llm_agent_vpc_link" {
  name_prefix = "${var.project_name}-vpc-link-"
  description = "Security group for LLM Agent VPC Link"

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-vpc-link-sg"
    Environment = var.environment
  }
}

# =====================================================
# Data Sources
# =====================================================

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# =====================================================
# API Gateway Integration
# =====================================================

resource "aws_apigatewayv2_integration" "llm_agent_http_integration" {
  api_id           = aws_apigatewayv2_api.llm_agent_api.id
  integration_type = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri  = "${var.instance_url}"
  payload_format_version = "1.0"

  request_parameters = {
    "overwrite:header.Host" = "instance.internal"
  }
}

# =====================================================
# API Gateway Routes
# =====================================================

# Health check route
resource "aws_apigatewayv2_route" "health_route" {
  api_id    = aws_apigatewayv2_api.llm_agent_api.id
  route_key = "GET /health"
  target    = "integrations/${aws_apigatewayv2_integration.llm_agent_http_integration.id}"
}

# Chat endpoint
resource "aws_apigatewayv2_route" "chat_route" {
  api_id    = aws_apigatewayv2_api.llm_agent_api.id
  route_key = "POST /api/v1/chat"
  target    = "integrations/${aws_apigatewayv2_integration.llm_agent_http_integration.id}"
}

# Statistics endpoint
resource "aws_apigatewayv2_route" "stats_route" {
  api_id    = aws_apigatewayv2_api.llm_agent_api.id
  route_key = "GET /api/v1/stats"
  target    = "integrations/${aws_apigatewayv2_integration.llm_agent_http_integration.id}"
}

# Conversation history endpoint
resource "aws_apigatewayv2_route" "conversation_get_route" {
  api_id    = aws_apigatewayv2_api.llm_agent_api.id
  route_key = "GET /api/v1/conversation/{id}"
  target    = "integrations/${aws_apigatewayv2_integration.llm_agent_http_integration.id}"
}

# Clear conversation endpoint
resource "aws_apigatewayv2_route" "conversation_delete_route" {
  api_id    = aws_apigatewayv2_api.llm_agent_api.id
  route_key = "DELETE /api/v1/conversation/{id}"
  target    = "integrations/${aws_apigatewayv2_integration.llm_agent_http_integration.id}"
}

# Catch-all route
resource "aws_apigatewayv2_route" "default_route" {
  api_id    = aws_apigatewayv2_api.llm_agent_api.id
  route_key = "$default"
  target    = "integrations/${aws_apigatewayv2_integration.llm_agent_http_integration.id}"
}

# =====================================================
# API Gateway Stage
# =====================================================

resource "aws_apigatewayv2_stage" "llm_agent_stage" {
  api_id      = aws_apigatewayv2_api.llm_agent_api.id
  name        = var.environment
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway_logs.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      sourceIp       = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      resourcePath   = "$context.resourcePath"
      status         = "$context.status"
      protocol       = "$context.protocol"
      responseLength = "$context.responseLength"
      integrationLatency = "$context.integration.latency"
    })
  }

  stage_variables = {
    instance_url = var.instance_url
  }

  tags = {
    Name        = "${var.project_name}-stage-${var.environment}"
    Environment = var.environment
  }
}

# =====================================================
# CloudWatch Logs
# =====================================================

resource "aws_cloudwatch_log_group" "api_gateway_logs" {
  name              = "/aws/apigateway/${var.project_name}-${var.environment}"
  retention_in_days = 7

  tags = {
    Name        = "${var.project_name}-api-logs"
    Environment = var.environment
  }
}

# =====================================================
# Outputs
# =====================================================

output "api_endpoint" {
  description = "API Gateway endpoint URL"
  value       = aws_apigatewayv2_stage.llm_agent_stage.invoke_url
}

output "api_id" {
  description = "API Gateway ID"
  value       = aws_apigatewayv2_api.llm_agent_api.id
}

output "stage_name" {
  description = "API Gateway stage name"
  value       = aws_apigatewayv2_stage.llm_agent_stage.name
}
