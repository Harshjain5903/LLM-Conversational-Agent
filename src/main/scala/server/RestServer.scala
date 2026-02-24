package server

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, JsonFormat, RootJsonFormat}
import com.typesafe.scalalogging.Logger
import config.AppConfig
import agent.ConversationAgent
import llm.LLMFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// JSON Models
case class ChatRequest(message: String, conversationId: String)
case class ChatResponse(response: String, conversationId: String, timestamp: String)
case class HealthCheckResponse(status: String, message: String)
case class ConversationStatsResponse(stats: Map[String, Any])

// JSON Protocols
object JsonProtocols extends DefaultJsonProtocol {
  implicit val chatRequestFormat: RootJsonFormat[ChatRequest] = jsonFormat2(ChatRequest)
  implicit val chatResponseFormat: RootJsonFormat[ChatResponse] = jsonFormat3(ChatResponse)
  implicit val healthCheckResponseFormat: RootJsonFormat[HealthCheckResponse] = jsonFormat2(HealthCheckResponse)
  implicit val conversationStatsFormat: RootJsonFormat[ConversationStatsResponse] = jsonFormat1(ConversationStatsResponse)
}

class RestServer(config: AppConfig)(implicit system: ActorSystem[Nothing]) {
  import JsonProtocols.*
  import spray.json.*

  private val logger = Logger("RestServer")
  private implicit val ec: ExecutionContext = system.executionContext

  // Initialize LLM Provider
  private val llmProvider = LLMFactory.create(config.llm.backend, config)
  
  // Initialize Conversation Agent
  private val agent = new ConversationAgent(llmProvider)

  def start(): Future[Http.ServerBinding] = {
    logger.info(s"Starting REST API Server on ${config.server.host}:${config.server.port}")
    
    val route = createRoutes()
    Http().newServerAt(config.server.host, config.server.port).bind(route)
  }

  private def createRoutes(): Route = {
    concat(
      // Health check endpoint
      get {
        path("health") {
          logger.debug("Health check request received")
          onComplete(llmProvider.healthCheck()) {
            case Success(isHealthy) =>
              val status = if (isHealthy) "healthy" else "unhealthy"
              complete(StatusCodes.OK -> HealthCheckResponse(status, "LLM Conversational Agent is running"))
            case Failure(ex) =>
              logger.error(s"Health check failed: ${ex.getMessage}", ex)
              complete(StatusCodes.ServiceUnavailable -> HealthCheckResponse("unhealthy", ex.getMessage))
          }
        }
      },

      // Chat endpoint
      post {
        path("api" / "v1" / "chat") {
          entity(as[ChatRequest]) { chatRequest =>
            logger.info(s"Chat request received for conversation: ${chatRequest.conversationId}")
            
            onComplete(agent.processMessage(chatRequest.conversationId, chatRequest.message)) {
              case Success(response) =>
                val timestamp = java.time.Instant.now().toString
                complete(StatusCodes.OK -> ChatResponse(response, chatRequest.conversationId, timestamp))
              
              case Failure(ex) =>
                logger.error(s"Error processing chat request: ${ex.getMessage}", ex)
                complete(StatusCodes.InternalServerError -> HttpEntity(
                  ContentTypes.`application/json`,
                  s"""{"error":"Error processing request: ${ex.getMessage}"}""".stripMargin
                ))
            }
          }
        }
      },

      // Statistics endpoint
      get {
        path("api" / "v1" / "stats") {
          logger.debug("Statistics request received")
          val stats = agent.getConversationStats()
          complete(StatusCodes.OK -> ConversationStatsResponse(stats))
        }
      },

      // Conversation history endpoint
      get {
        path("api" / "v1" / "conversation" / Segment) { conversationId =>
          logger.debug(s"Conversation history request for: $conversationId")
          agent.getConversation(conversationId) match {
            case Some(context) =>
              complete(StatusCodes.OK -> HttpEntity(
                ContentTypes.`application/json`,
                context.toJson.toString()
              ))
            case None =>
              complete(StatusCodes.NotFound -> HttpEntity(
                ContentTypes.`application/json`,
                s"""{"error":"Conversation not found: $conversationId"}"""
              ))
          }
        }
      },

      // Root endpoint
      get {
        path("") {
          complete(StatusCodes.OK -> HttpEntity(
            ContentTypes.`application/json`,
            """{"name":"LLM Conversational Agent","version":"1.0.0","status":"running"}"""
          ))
        }
      },

      // Fallback for undefined routes
      complete(StatusCodes.NotFound -> HttpEntity(
        ContentTypes.`application/json`,
        """{"error":"Endpoint not found"}"""
      ))
    )
  }

  implicit def konversionFormat: JsonFormat[llm.ConversationContext] = new JsonFormat[llm.ConversationContext] {
    override def write(obj: llm.ConversationContext): spray.json.JsValue = {
      JsObject(
        "conversationId" -> JsString(obj.conversationId),
        "messagesCount" -> JsNumber(obj.messages.length),
        "messages" -> JsArray(obj.messages.map(msg =>
          JsObject(
            "role" -> JsString(msg.role),
            "content" -> JsString(msg.content)
          )
        ).toVector)
      )
    }

    override def read(json: spray.json.JsValue): llm.ConversationContext = ???
  }
}
