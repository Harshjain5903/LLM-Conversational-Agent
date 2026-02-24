# AWS Lambda Integration
# Serverless adapter for LLM Conversational Agent
# Author: Harsh Jain

# =====================================================
# IAM Role for Lambda
# =====================================================

resource "aws_iam_role" "lambda_role" {
  name_prefix = "llm-agent-lambda-"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Allow Lambda to call EC2 instance
resource "aws_iam_role_policy" "lambda_ec2_invoke" {
  name = "llm-agent-lambda-ec2-invoke"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeNetworkInterfaces"
        ]
        Resource = "*"
      }
    ]
  })
}

# =====================================================
# Lambda Layer (optional - for shared libraries)
# =====================================================

resource "aws_lambda_layer_version" "agent_dependencies" {
  filename   = "lambda_layer.zip"
  layer_name = "llm-agent-dependencies"

  source_code_hash = filebase64sha256("lambda_layer.zip")

  compatible_runtimes = ["java11", "java17"]
}

# =====================================================
# Lambda Function - Chat Handler
# =====================================================

resource "aws_lambda_function" "chat_handler" {
  function_name = "llm-agent-chat-handler"
  role          = aws_iam_role.lambda_role.arn
  handler       = "index.handler"
  runtime       = "nodejs18.x"
  timeout       = 60
  memory_size   = 512

  filename = "lambda_chat_handler.zip"

  environment {
    variables = {
      AGENT_ENDPOINT = var.instance_url
      ENVIRONMENT    = var.environment
    }
  }

  layers = [aws_lambda_layer_version.agent_dependencies.arn]

  vpc_config {
    subnet_ids         = data.aws_subnets.default.ids
    security_group_ids = [aws_security_group.lambda_security_group.id]
  }

  depends_on = [aws_iam_role_policy_attachment.lambda_basic_execution]
}

# =====================================================
# Lambda Function - Stats Handler
# =====================================================

resource "aws_lambda_function" "stats_handler" {
  function_name = "llm-agent-stats-handler"
  role          = aws_iam_role.lambda_role.arn
  handler       = "index.handler"
  runtime       = "nodejs18.x"
  timeout       = 30
  memory_size   = 256

  filename = "lambda_stats_handler.zip"

  environment {
    variables = {
      AGENT_ENDPOINT = var.instance_url
      ENVIRONMENT    = var.environment
    }
  }

  depends_on = [aws_iam_role_policy_attachment.lambda_basic_execution]
}

# =====================================================
# Security Group for Lambda
# =====================================================

resource "aws_security_group" "lambda_security_group" {
  name_prefix = "llm-agent-lambda-"
  description = "Security group for Lambda functions"

  egress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/8"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# =====================================================
# Lambda Permissions for API Gateway
# =====================================================

resource "aws_lambda_permission" "chat_api_gateway" {
  statement_id  = "AllowAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.chat_handler.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.llm_agent_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "stats_api_gateway" {
  statement_id  = "AllowAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.stats_handler.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.llm_agent_api.execution_arn}/*/*"
}

# =====================================================
# CloudWatch Alarms for Lambda
# =====================================================

resource "aws_cloudwatch_metric_alarm" "lambda_chat_errors" {
  alarm_name          = "llm-agent-lambda-chat-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = "300"
  statistic           = "Sum"
  threshold           = "5"
  alarm_description   = "This alarm monitors Lambda chat handler errors"

  dimensions = {
    FunctionName = aws_lambda_function.chat_handler.function_name
  }
}

resource "aws_cloudwatch_metric_alarm" "lambda_chat_throttles" {
  alarm_name          = "llm-agent-lambda-chat-throttles"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Throttles"
  namespace           = "AWS/Lambda"
  period              = "300"
  statistic           = "Sum"
  threshold           = "0"
  alarm_description   = "This alarm monitors Lambda throttling"

  dimensions = {
    FunctionName = aws_lambda_function.chat_handler.function_name
  }
}

# =====================================================
# Outputs
# =====================================================

output "chat_handler_arn" {
  description = "Chat handler Lambda function ARN"
  value       = aws_lambda_function.chat_handler.arn
}

output "stats_handler_arn" {
  description = "Stats handler Lambda function ARN"
  value       = aws_lambda_function.stats_handler.arn
}
