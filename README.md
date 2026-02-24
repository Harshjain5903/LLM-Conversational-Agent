# LLM Conversational Agent

A scalable, production-ready AI conversational agent built with Scala microservices, Akka HTTP RESTful APIs, and gRPC. Integrated with Amazon Bedrock and Ollama for advanced LLM capabilities, containerized with Docker, and deployed on AWS EC2.

## Project Overview

This project demonstrates a sophisticated architecture for building and deploying an enterprise-grade conversational AI agent. It showcases modern cloud-native development practices including microservices architecture, high-performance concurrency handling, serverless integration, and distributed communication patterns.

## Architecture

### Technology Stack

- **Language**: Scala 3.x
- **Framework**: Akka HTTP
- **Communication**: gRPC
- **LLM Integration**: Amazon Bedrock, Ollama
- **Container**: Docker
- **Cloud**: AWS (EC2, API Gateway, Lambda)
- **Concurrency**: Akka Actors for distributed processing

### Key Components

1. **REST API Server** - Akka HTTP-based RESTful API for handling client requests
2. **gRPC Services** - High-performance inter-service communication using Protocol Buffers
3. **LLM Integration Layer** - Abstractions for both Amazon Bedrock and Ollama
4. **Agent Orchestration** - Scala microservices managing conversational flows
5. **Configuration Management** - Externalized configuration for different deployment environments

## Features

- **Conversational Capabilities**: Multi-turn dialogue with context awareness
- **LLM Flexibility**: Switch between Amazon Bedrock and Ollama backends
- **High Performance**: Akka-based concurrent request handling
- **Microservices Architecture**: Scalable, independently deployable services
- **Containerized**: Docker support for consistent deployment
- **Cloud-Native**: AWS EC2 deployment with API Gateway and Lambda integration
- **Real-time Processing**: Efficient handling of concurrent user interactions

## Project Structure

```
├── README.md
├── build.sbt
├── Dockerfile
├── docker-compose.yml
├── LICENSE
├── src/
│   ├── main/
│   │   ├── protobuf/          # Protocol Buffer definitions for gRPC
│   │   ├── scala/
│   │   │   ├── main.scala     # Application entry point
│   │   │   ├── server/        # REST API server
│   │   │   ├── service/       # gRPC services
│   │   │   ├── agent/         # Agent orchestration
│   │   │   ├── llm/           # LLM integrations
│   │   │   └── config/        # Configuration management
│   │   └── resources/
│   │       └── application.conf
│   └── test/
│       └── scala/
└── deployment/
    └── aws-setup.md
```

## Quick Start

### Prerequisites

- Scala 3.x
- JDK 11 or higher
- Docker and Docker Compose (for containerized deployment)
- AWS account with Bedrock access (for production)
- Ollama (for local LLM inference)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/Harshjain5903/LLM-Conversational-Agent
   cd LLM-Conversational-Agent
   ```

2. **Build the project**
   ```bash
   sbt clean compile
   ```

3. **Run the REST API server**
   ```bash
   sbt run
   ```

4. **Run with Docker Compose**
   ```bash
   docker-compose up --build
   ```

### Configuration

Configure the agent using `src/main/resources/application.conf`:

```hocon
app {
  server {
    host = "0.0.0.0"
    port = 8080
  }
  
  llm {
    backend = "bedrock"  # or "ollama"
    
    bedrock {
      region = "us-east-1"
      model = "anthropic.claude-3-sonnet-20240229-v1:0"
    }
    
    ollama {
      endpoint = "http://localhost:11434"
      model = "llama2"
    }
  }
}
```

## Deployment

### Docker

The project includes a Dockerfile for containerized deployment:

```bash
# Build image
docker build -t llm-agent:latest .

# Run container
docker run -p 8080:8080 llm-agent:latest
```

### AWS EC2

See [aws-setup.md](deployment/aws-setup.md) for detailed AWS EC2 deployment instructions including:
- EC2 instance setup
- Docker container deployment
- API Gateway integration
- Lambda function configuration
- IAM role and permission setup

## API Endpoints

### REST API

**POST** `/api/v1/chat` - Send a message and get AI response

```json
{
  "message": "What is machine learning?",
  "conversationId": "conv-123"
}
```

**Response:**
```json
{
  "response": "Machine learning is a subset of artificial intelligence...",
  "conversationId": "conv-123",
  "timestamp": "2025-09-15T10:30:00Z"
}
```

## Performance

- **Concurrency**: Handles thousands of concurrent requests via Akka
- **Latency**: Sub-second response times for conversational queries
- **Throughput**: High message processing rate with distributed processing

## Development

### Testing

```bash
sbt test
```

### Code Style

The project follows Scala best practices:
- Functional programming patterns
- Immutable data structures
- Strong type safety
- Comprehensive error handling

## Logging and Monitoring

- **Logging Framework**: SLF4J + Logback
- **Log Levels**: Configurable per module (TRACE, DEBUG, INFO, WARN, ERROR)
- **CloudWatch Integration**: AWS monitoring for cloud deployments

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

**Harsh Jain**

Connect on GitHub: [@Harshjain5903](https://github.com/Harshjain5903)

## Acknowledgments

- Amazon Bedrock for advanced LLM capabilities
- Ollama for local LLM inference
- Akka framework for high-performance concurrency
- Scala community for functional programming patterns
