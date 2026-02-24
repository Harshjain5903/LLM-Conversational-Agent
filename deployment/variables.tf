# Terraform Variables
# Configure these based on your AWS setup

variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
  
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be dev, staging, or prod."
  }
}

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "llm-agent"
}

variable "instance_url" {
  description = "EC2 instance URL with port (e.g., http://10.0.0.100:8080)"
  type        = string
  
  validation {
    condition     = can(regex("^https?://", var.instance_url))
    error_message = "Instance URL must start with http:// or https://"
  }
}

variable "enable_lambda" {
  description = "Enable Lambda for serverless execution"
  type        = bool
  default     = false
}

variable "enable_monitoring" {
  description = "Enable CloudWatch monitoring and alarms"
  type        = bool
  default     = true
}

variable "api_throttle_settings" {
  description = "API throttle settings"
  type = object({
    burst_limit = number
    rate_limit  = number
  })
  default = {
    burst_limit = 5000
    rate_limit  = 2000
  }
}

variable "lambda_memory" {
  description = "Lambda function memory in MB"
  type        = number
  default     = 512
  
  validation {
    condition     = var.lambda_memory >= 128 && var.lambda_memory <= 3008
    error_message = "Lambda memory must be between 128 and 3008 MB."
  }
}

variable "lambda_timeout" {
  description = "Lambda function timeout in seconds"
  type        = number
  default     = 60
  
  validation {
    condition     = var.lambda_timeout > 0 && var.lambda_timeout <= 900
    error_message = "Lambda timeout must be between 1 and 900 seconds."
  }
}

variable "tags" {
  description = "Common tags for all resources"
  type        = map(string)
  default = {
    Project     = "LLM-Conversational-Agent"
    Terraform   = "true"
    Author      = "Harsh Jain"
    ManagedBy   = "Terraform"
  }
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
  
  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653], var.log_retention_days)
    error_message = "Log retention must be a valid CloudWatch value."
  }
}
