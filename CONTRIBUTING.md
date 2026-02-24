# Contributing to LLM Conversational Agent

Thank you for your interest in contributing to the LLM Conversational Agent project! This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and professional
- Provide constructive feedback
- Focus on the code, not the person
- Help others when possible

## Getting Started

### Prerequisites
- OpenJDK 11+
- Scala 3.5.0
- SBT 1.9.7+
- Docker (optional)

### Setup Development Environment

```bash
# Clone the repository
git clone https://github.com/Harshjain5903/LLM-Conversational-Agent.git
cd LLM-Conversational-Agent

# Verify build works
sbt clean compile test
```

## Development Workflow

### Branch Naming
- `feature/description` - for new features
- `fix/description` - for bug fixes
- `docs/description` - for documentation updates
- `refactor/description` - for code refactoring

### Code Style

This project follows Scala best practices:
- **Functional Programming**: Prefer immutable data structures and functional patterns
- **Type Safety**: Use strong typing, avoid `Any` and `Option` anti-patterns
- **Error Handling**: Use `Either`, `Try`, or `Future` for error handling
- **Naming Conventions**: Follow Scala naming conventions (camelCase for methods, PascalCase for classes)

### Formatting

Format code before committing:

```bash
sbt scalafmt
```

### Testing

All new code must include tests:

```bash
# Run all tests
sbt test

# Run specific test
sbt testOnly com.hardas.agent.ConversationAgentTest

# Run with coverage
sbt coverage test coverageReport
```

Minimum test coverage requirement: **70%**

## Making a Pull Request

1. **Create a Feature Branch**
   ```bash
   git checkout -b feature/your-feature
   ```

2. **Make Changes**
   - Write clean, well-commented code
   - Add/update tests as needed
   - Update documentation if applicable

3. **Test Your Changes**
   ```bash
   sbt clean test
   ```

4. **Format Code**
   ```bash
   sbt scalafmt
   ```

5. **Commit with Clear Messages**
   ```bash
   git commit -m "feat: Add new feature"
   git commit -m "fix: Resolve issue with X"
   git commit -m "docs: Update README"
   ```

6. **Push to Your Fork**
   ```bash
   git push origin feature/your-feature
   ```

7. **Open a Pull Request**
   - Provide clear description of changes
   - Reference related issues
   - Include any relevant testing information

## Commit Message Guidelines

Use the following format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation change
- `refactor`: Code refactoring
- `test`: Test addition/modification
- `chore`: Maintenance

**Example**:
```
feat(llm): Add Cohere integration support

Add support for Amazon Cohere models through Bedrock.
- Implement CohereLLMProvider
- Add configuration for Cohere models
- Include integration tests

Closes #42
```

## Architecture Guidelines

### Adding New LLM Providers

To add a new LLM provider:

1. Extend `LLMProvider` trait in `llm/` package
2. Implement `generateResponse()` and `healthCheck()` methods
3. Add configuration to `AppConfig.scala`
4. Update `LLMFactory.create()` method
5. Add tests in `src/test/scala/llm/`
6. Update README with new provider documentation

### Adding New API Endpoints

To add new REST endpoints:

1. Add route to `RestServer.scala`
2. Define request/response case classes
3. Add JSON protocols
4. Implement route handler with proper error handling
5. Add tests in `src/test/scala/server/`
6. Update API documentation in README

## Issues

### Reporting Bugs

Use GitHub Issues to report bugs. Include:
- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, Scala version, etc.)

### Feature Requests

For feature requests:
- Describe the feature and its benefits
- Provide use cases
- Explain how it fits with the project goals

## Documentation

- Keep README.md up to date
- Add comments for complex logic
- Include docstrings for public APIs
- Update deployment guides when architecture changes

## Performance Considerations

When contributing code, consider:
- Async processing where applicable
- Resource cleanup (connections, files, etc.)
- Memory efficiency
- Latency impact on concurrent operations

## Security

- Do not commit secrets, credentials, or API keys
- Use environment variables for sensitive configuration
- Validate all external inputs
- Report security issues privately to the maintainer

## Questions?

- Open an issue for discussions
- Tag with `question` label
- Check existing discussions first

## Thank You!

Your contributions make this project better. Thank you for helping improve the LLM Conversational Agent!

---

**Last Updated**: February 2025  
**Maintainer**: Harsh Jain [@Harshjain5903](https://github.com/Harshjain5903)
