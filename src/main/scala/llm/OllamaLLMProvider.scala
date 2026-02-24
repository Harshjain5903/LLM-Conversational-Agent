/**
 * Ollama LLM Provider
 * ==================
 * Integrates with Ollama for local/on-premise LLM inference.
 * Provides HTTP-based communication with Ollama endpoints.
 *
 * Author: Harsh Jain
 */

package llm

import com.typesafe.scalalogging.Logger
import sttp.client3.*

import scala.concurrent.{ExecutionContext, Future}

class OllamaLLMProvider(config: config.OllamaConfig)(implicit ec: ExecutionContext) extends LLMProvider {
  private val logger = Logger("OllamaLLMProvider")
  
  private implicit val backend: SttpBackend[Future, _] = HttpClientFutureBackend()

  logger.info(s"Ollama LLM Provider initialized with endpoint: ${config.endpoint}, model: ${config.model}")

  override def generateResponse(prompt: String): Future[String] = {
    try {
      val escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n")
      val requestBody = s"""{"model":"${config.model}","prompt":"$escapedPrompt","stream":false}"""
      
      val request = basicRequest
        .post(uri"${config.endpoint}/api/generate")
        .body(requestBody)
        .header("Content-Type", "application/json")
        .readTimeout(java.time.Duration.ofSeconds(60))

      request.send(backend).flatMap { resp =>
        if (resp.code.isSuccess) {
          logger.debug(s"Ollama response received for prompt: '${prompt.take(50)}...'")
          Future.successful(resp.body.fold(
            err => {
              logger.error(s"Ollama response parsing error: $err")
              "Error: Unable to parse response"
            },
            success => success
          ))
        } else {
          logger.error(s"Ollama API error: ${resp.code}")
          Future.failed(new RuntimeException(s"Ollama API returned ${resp.code}"))
        }
      }.recoverWith {
        case ex =>
          logger.error(s"Error invoking Ollama: ${ex.getMessage}", ex)
          Future.failed(ex)
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Exception in Ollama request: ${ex.getMessage}", ex)
        Future.failed(ex)
    }
  }

  override def healthCheck(): Future[Boolean] = {
    try {
      val request = basicRequest
        .get(uri"${config.endpoint}/api/tags")
        .readTimeout(java.time.Duration.ofSeconds(10))

      request.send(backend).map { resp =>
        val isHealthy = resp.code.isSuccess
        if (isHealthy) {
          logger.info("Ollama health check passed")
        } else {
          logger.warn(s"Ollama health check failed: ${resp.code}")
        }
        isHealthy
      }.recoverWith {
        case ex =>
          logger.error(s"Ollama health check error: ${ex.getMessage}", ex)
          Future.successful(false)
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Exception in Ollama health check: ${ex.getMessage}", ex)
        Future.successful(false)
    }
  }

  def shutdown(): Unit = {
    backend.close()
    logger.info("Ollama LLM Provider shut down")
  }
}

