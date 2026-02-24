# GETTING_STARTED.md

# LLM Conversational Agent - Getting Started Guide

**Project Status**: ✅ COMPLETE & READY TO USE

This is a **fully built, production-ready project**. No configuration needed - just download and run.

---

## ⚡ Quickest Start (2 minutes)

```bash
# 1. Clone the project
git clone https://github.com/Harshjain5903/LLM-Conversational-Agent.git
cd LLM-Conversational-Agent

# 2. Start everything
docker-compose up --build

# 3. Test it's working (in another terminal)
curl http://localhost:8080/health
```

**Done!** Your API is running at `http://localhost:8080`

---

## 🚀 First API Call (Copy & Paste)

### Test Chat Endpoint
```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is machine learning?", "conversationId": "user-1"}'
```

**Expected Response:**
```json
{
  "response": "Machine learning is a subset of artificial intelligence...",
  "conversationId": "user-1",
  "timestamp": "2025-02-24T10:30:00Z"
}
```

---

## 🎮 Interactive Demo

```bash
# Make script executable
chmod +x demo.sh

# Run interactive chat
./demo.sh
```

Then type messages and chat with the AI. Type `quit` to exit.

---

## 🧪 Automated Tests

```bash
# Make script executable
chmod +x test-api.sh

# Run all tests
./test-api.sh
```

This runs automatic tests on all endpoints and shows results.

---

## 📊 Available Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/health` | GET | Check system health |
| `/api/v1/chat` | POST | Send message and get response |
| `/api/v1/stats` | GET | View conversation statistics |
| `/api/v1/conversation/{id}` | GET | View conversation history |
| `/api/v1/conversation/{id}` | DELETE | Clear conversation |

---

## 🐳 Docker Commands

```bash
# Start everything
docker-compose up --build

# View logs
docker-compose logs -f llm-agent

# Stop everything
docker-compose down

# Rebuild without cache
docker-compose build --no-cache
```

---

## 💻 Run Without Docker (If SBT Installed)

```bash
# Start API server
sbt run

# In another terminal, run Ollama
ollama serve
```

---

## ✅ Verify Everything Works

### 1. API Running?
```bash
curl http://localhost:8080/health
# Should return: {"status":"healthy","message":"LLM Conversational Agent is running"}
```

### 2. Chat Working?
```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","conversationId":"test"}'
# Should return AI response
```

### 3. View Logs?
```bash
docker-compose logs llm-agent
# Should show request activity
```

---

## 🔧 Configuration

All settings are in `src/main/resources/application.conf`:

```hocon
app {
  server {
    host = "0.0.0.0"
    port = 8080
  }
  
  llm {
    backend = "ollama"  # or "bedrock" for AWS
    
    ollama {
      endpoint = "http://localhost:11434"
      model = "llama2"
    }
  }
}
```

**No change needed** - defaults work perfectly!

---

## 🌐 What's Included

| Component | Status |
|-----------|--------|
| REST API | ✅ Running |
| Akka HTTP | ✅ Working |
| Ollama Integration | ✅ Configured |
| AWS Bedrock | ✅ Ready |
| Multi-turn Chat | ✅ Working |
| Error Handling | ✅ Complete |
| Docker | ✅ Ready |
| Tests | ✅ Included |
| Logging | ✅ Enabled |
| Production Ready | ✅ Yes |

---

## 📁 What's in the Box

```
├── src/main/scala/           # Complete source code
├── src/test/scala/           # Test suite
├── Dockerfile                # Production image
├── docker-compose.yml        # Local dev setup
├── quick-start.sh            # Auto-start script
├── test-api.sh               # API test script
├── demo.sh                   # Interactive demo
├── README.md                 # Main documentation
└── build.sbt                 # Dependencies
```

---

## 🆘 Troubleshooting

### Port 8080 Already in Use?
```bash
# Run on different port
docker run -p 9090:8080 llm-agent:latest
```

### Docker Not Installed?
- Download from: https://www.docker.com

### Ollama Not Working?
```bash
# Ollama is installed inside Docker - no separate install needed
# If using local: brew install ollama && ollama serve
```

### API Timeout?
- Wait a bit, first request can be slow
- Check logs: `docker-compose logs llm-agent`

---

## 📖 More Information

- **Full README**: See [README.md](README.md)
- **AWS Deployment**: See [deployment/aws-setup.md](deployment/aws-setup.md)
- **Contributing**: See [CONTRIBUTING.md](CONTRIBUTING.md)
- **Changelog**: See [CHANGELOG.md](CHANGELOG.md)

---

## 🎯 Next Steps

1. ✅ Start the project (`docker-compose up --build`)
2. ✅ Test an endpoint (`curl http://localhost:8080/health`)
3. ✅ Send a message (`curl http://localhost:8080/api/v1/chat`)
4. ✅ Run demo script (`./demo.sh`)
5. ✅ Review the code (`src/main/scala/`)
6. ✅ Deploy to production (see aws-setup.md)

---

**Author**: Harsh Jain  
**GitHub**: [@Harshjain5903](https://github.com/Harshjain5903)  
**Project**: [LLM Conversational Agent](https://github.com/Harshjain5903/LLM-Conversational-Agent)

---

**Everything is ready to use right now. No setup required!**
