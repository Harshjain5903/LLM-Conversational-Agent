/**
 * Application Configuration Management
 * ====================================
 * Manages externalized configuration for the LLM Conversational Agent.
 * Configuration is loaded from application.conf with environment variable overrides.
 *
 * Author: Harsh Jain
 */

package config

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger

case class ServerConfig(
  host: String,
  port: Int
)

case class BedrockConfig(
  region: String,
  model: String
)

case class OllamaConfig(
  endpoint: String,
  model: String
)

case class LLMConfig(
  backend: String,  // "bedrock" or "ollama"
  bedrock: BedrockConfig,
  ollama: OllamaConfig
)

case class AppConfig(
  server: ServerConfig,
  llm: LLMConfig
)

object AppConfig {
  private val logger = Logger("AppConfig")

  def load(): AppConfig = {
    val config = ConfigFactory.load()
    
    try {
      val serverHost = config.getString("app.server.host")
      val serverPort = config.getInt("app.server.port")
      
      val llmBackend = config.getString("app.llm.backend")
      val bedrockRegion = config.getString("app.llm.bedrock.region")
      val bedrockModel = config.getString("app.llm.bedrock.model")
      val ollamaEndpoint = config.getString("app.llm.ollama.endpoint")
      val ollamaModel = config.getString("app.llm.ollama.model")
      
      logger.info(s"Configuration loaded: server=$serverHost:$serverPort, llm-backend=$llmBackend")
      
      AppConfig(
        server = ServerConfig(host = serverHost, port = serverPort),
        llm = LLMConfig(
          backend = llmBackend,
          bedrock = BedrockConfig(region = bedrockRegion, model = bedrockModel),
          ollama = OllamaConfig(endpoint = ollamaEndpoint, model = ollamaModel)
        )
      )
    } catch {
      case ex: Exception =>
        logger.error(s"Failed to load configuration: ${ex.getMessage}", ex)
        throw new RuntimeException("Configuration loading failed", ex)
    }
  }
}
