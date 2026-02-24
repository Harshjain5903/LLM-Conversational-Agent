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
  
  try {
    // Load configuration
    val config = AppConfig.load()
    logger.info("Configuration loaded successfully")
    logger.info(s"Starting REST server on ${config.server.host}:${config.server.port}")
    logger.info(s"LLM Backend: ${config.llm.backend}")

    // Start REST API Server
    val restServer = new RestServer(config)
    val bindingFuture = restServer.start()

    bindingFuture onComplete {
      case Success(binding) =>
        logger.info(s"REST API Server started: ${binding.localAddress}")
        logger.info("LLM Conversational Agent is running successfully")
      case Failure(ex) =>
        logger.error(s"Failed to start REST API Server: ${ex.getMessage}", ex)
        system.terminate()
    }

    // Graceful shutdown hook
    scala.sys.addShutdownHook {
      logger.info("Shutting down LLM Conversational Agent...")
      bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  } catch {
    case ex: Exception =>
      logger.error(s"Fatal error during startup: ${ex.getMessage}", ex)
      system.terminate()
  }
}

