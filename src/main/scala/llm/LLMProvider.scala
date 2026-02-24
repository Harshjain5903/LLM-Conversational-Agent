package llm

import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

trait LLMProvider {
  def generateResponse(prompt: String): Future[String]
  def healthCheck(): Future[Boolean]
}

case class ConversationMessage(
  role: String,  // "user" or "assistant"
  content: String
)

case class ConversationContext(
  conversationId: String,
  messages: List[ConversationMessage] = List(),
  metadata: Map[String, String] = Map()
)

object LLMFactory {
  private val logger = Logger("LLMFactory")

  def create(backend: String, config: config.AppConfig): LLMProvider = {
    backend.toLowerCase match {
      case "bedrock" =>
        logger.info("Initializing Bedrock LLM provider")
        new BedrockLLMProvider(config.llm.bedrock)
      case "ollama" =>
        logger.info("Initializing Ollama LLM provider")
        new OllamaLLMProvider(config.llm.ollama)
      case _ =>
        logger.error(s"Unknown LLM backend: $backend")
        throw new IllegalArgumentException(s"Unsupported LLM backend: $backend")
    }
  }
}
