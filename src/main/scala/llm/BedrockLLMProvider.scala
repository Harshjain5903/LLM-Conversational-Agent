/**
 * Amazon Bedrock LLM Provider
 * ===========================
 * Integrates with AWS Bedrock for enterprise-grade LLM capabilities.
 * Handles invocation and response management for Claude and other Bedrock models.
 *
 * Author: Harsh Jain
 */

package llm

import com.typesafe.scalalogging.Logger
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import software.amazon.awssdk.core.SdkBytes

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.FutureConverters._

class BedrockLLMProvider(config: config.BedrockConfig)(implicit ec: ExecutionContext) extends LLMProvider {
  private val logger = Logger("BedrockLLMProvider")
  
  private val client: BedrockRuntimeClient = BedrockRuntimeClient.builder()
    .region(software.amazon.awssdk.regions.Region.of(config.region))
    .build()

  logger.info(s"Bedrock LLM Provider initialized with model: ${config.model}")

  override def generateResponse(prompt: String): Future[String] = {
    Future {
      blocking {
        try {
          val request = InvokeModelRequest.builder()
            .modelId(config.model)
            .body(SdkBytes.fromUtf8String(s"""{"prompt":"$prompt","max_tokens":512}"""))
            .build()

          val response = client.invokeModel(request)
          val responseBody = response.body().asUtf8String()
          
          logger.debug(s"Bedrock response received for prompt: '${prompt.take(50)}...'")
          responseBody
        } catch {
          case ex: Exception =>
            logger.error(s"Error invoking Bedrock model: ${ex.getMessage}", ex)
            throw ex
        }
      }
    }
  }

  override def healthCheck(): Future[Boolean] = {
    Future {
      try {
        val testRequest = InvokeModelRequest.builder()
          .modelId(config.model)
          .body(SdkBytes.fromUtf8String("""{"prompt":"health check","max_tokens":10}"""))
          .build()
        
        client.invokeModel(testRequest)
        logger.info("Bedrock health check passed")
        true
      } catch {
        case ex: Exception =>
          logger.error(s"Bedrock health check failed: ${ex.getMessage}", ex)
          false
      }
    }
  }

  def shutdown(): Unit = {
    client.close()
    logger.info("Bedrock LLM Provider shut down")
  }
}
