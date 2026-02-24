/**
 * LLM Conversational Agent
 * ========================
 * A production-grade enterprise AI conversational agent built with Scala microservices,
 * Akka HTTP RESTful APIs, and gRPC integration with Amazon Bedrock and Ollama.
 *
 * Author: Harsh Jain
 * GitHub: https://github.com/Harshjain5903
 * License: MIT
 *
 * Main Application Entry Point
 */

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import server.RestServer
import config.AppConfig

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

@main
def main(): Unit = {
  implicit val system: ActorSystem[Nothing] = ActorSystem(
    akka.actor.typed.Behavior.empty[Nothing],
    "LLMConversationalAgent"
  )
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val logger = Logger("LLMConversationalAgent")
  
  logger.info("=" * 80)
  logger.info("LLM Conversational Agent - Starting Application")
  logger.info("=" * 80)
  logger.info(s"Author: Harsh Jain")
  logger.info(s"Version: 1.0.0")
  logger.info(s"Timestamp: ${java.time.Instant.now()}")
  
  try {
    // Load configuration
    val config = AppConfig.load()
    logger.info("✓ Configuration loaded successfully")
    logger.info(s"  - Server: ${config.server.host}:${config.server.port}")
    logger.info(s"  - LLM Backend: ${config.llm.backend}")

    // Start REST API Server
    val restServer = new RestServer(config)
    val bindingFuture = restServer.start()

    bindingFuture onComplete {
      case Success(binding) =>
        logger.info(s"✓ REST API Server started successfully")
        logger.info(s"  - Listen Address: ${binding.localAddress}")
        logger.info("✓ LLM Conversational Agent is running and ready for requests")
        logger.info("=" * 80)
      
      case Failure(ex) =>
        logger.error(s"✗ Failed to start REST API Server: ${ex.getMessage}", ex)
        logger.error("=" * 80)
        system.terminate()
    }

    // Graceful shutdown hook
    scala.sys.addShutdownHook {
      logger.info("=" * 80)
      logger.info("Shutting down LLM Conversational Agent...")
      bindingFuture.flatMap(_.unbind()).onComplete { _ =>
        system.terminate()
        logger.info("✓ Application shut down successfully")
        logger.info("=" * 80)
      }
    }
  } catch {
    case ex: Exception =>
      logger.error(s"✗ Fatal error during startup: ${ex.getMessage}", ex)
      logger.error("=" * 80)
      system.terminate()
  }
}

