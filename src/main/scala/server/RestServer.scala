/**
 * REST API Server
 * ===============
 * Akka HTTP-based REST API server for the LLM Conversational Agent.
 * Provides endpoints for chat, health checks, statistics, and conversation management.
 *
 * Author: Harsh Jain
 */

package server

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import spray.json.*
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
case class ErrorResponse(error: String)

// JSON Protocols
object JsonProtocols extends DefaultJsonProtocol {
  implicit val chatRequestFormat: RootJsonFormat[ChatRequest] = jsonFormat2(ChatRequest)
  implicit val chatResponseFormat: RootJsonFormat[ChatResponse] = jsonFormat3(ChatResponse)
  implicit val healthCheckResponseFormat: RootJsonFormat[HealthCheckResponse] = jsonFormat2(HealthCheckResponse)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse)
  implicit val anyFormat: RootJsonFormat[Any] = new RootJsonFormat[Any] {
    override def write(value: Any): JsValue = value match {
      case n: Int => JsNumber(n)
      case d: Double => JsNumber(d)
      case l: Long => JsNumber(l)
      case s: String => JsString(s)
      case true => JsTrue
      case false => JsFalse
      case null => JsNull
      case _ => JsString(value.toString)
    }
    override def read(json: JsValue): Any = ???
  }
}

class RestServer(config: AppConfig)(implicit system: ActorSystem[Nothing]) {
  import JsonProtocols.*

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
      // Root endpoint
      get {
        path("") {
          complete(StatusCodes.OK -> HttpEntity(
            ContentTypes.`application/json`,
            """{"name":"LLM Conversational Agent","version":"1.0.0","status":"running"}"""
          ))
        }
      },

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
              complete(StatusCodes.ServiceUnavailable -> HealthCheckResponse("unhealthy", s"Health check failed: ${ex.getMessage}"))
          }
        }
      },

      // Chat endpoint
      post {
        path("api" / "v1" / "chat") {
          entity(as[ChatRequest]) { chatRequest =>
            logger.info(s"Chat request received for conversation: ${chatRequest.conversationId}")
            
            if (chatRequest.message.isBlank) {
              complete(StatusCodes.BadRequest -> ErrorResponse("Message cannot be empty"))
            } else {
              onComplete(agent.processMessage(chatRequest.conversationId, chatRequest.message)) {
                case Success(response) =>
                  val timestamp = java.time.Instant.now().toString
                  complete(StatusCodes.OK -> ChatResponse(response, chatRequest.conversationId, timestamp))
                
                case Failure(ex) =>
                  logger.error(s"Error processing chat request: ${ex.getMessage}", ex)
                  complete(StatusCodes.InternalServerError -> ErrorResponse(s"Error processing request: ${ex.getMessage}"))
              }
            }
          }
        }
      },

      // Statistics endpoint
      get {
        path("api" / "v1" / "stats") {
          logger.debug("Statistics request received")
          val stats = agent.getConversationStats()
          val json = stats.toJson.toString()
          complete(StatusCodes.OK -> HttpEntity(
            ContentTypes.`application/json`,
            json
          ))
        }
      },

      // Conversation history endpoint
      get {
        path("api" / "v1" / "conversation" / Segment) { conversationId =>
          logger.debug(s"Conversation history request for: $conversationId")
          agent.getConversation(conversationId) match {
            case Some(context) =>
              val json = JsObject(
                "conversationId" -> JsString(context.conversationId),
                "messagesCount" -> JsNumber(context.messages.length),
                "messages" -> JsArray(context.messages.map(msg =>
                  JsObject(
                    "role" -> JsString(msg.role),
                    "content" -> JsString(msg.content)
                  )
                ).toVector)
              ).toString()
              complete(StatusCodes.OK -> HttpEntity(
                ContentTypes.`application/json`,
                json
              ))
            case None =>
              logger.warn(s"Conversation not found: $conversationId")
              complete(StatusCodes.NotFound -> ErrorResponse(s"Conversation not found: $conversationId"))
          }
        }
      },

      // Clear conversation endpoint
      delete {
        path("api" / "v1" / "conversation" / Segment) { conversationId =>
          logger.info(s"Clearing conversation: $conversationId")
          if (agent.clearConversation(conversationId)) {
            complete(StatusCodes.OK -> HttpEntity(
              ContentTypes.`application/json`,
              s"""{"message":"Conversation $conversationId cleared"}"""
            ))
          } else {
            complete(StatusCodes.NotFound -> ErrorResponse(s"Conversation not found: $conversationId"))
          }
        }
      },

      // Fallback for undefined routes
      complete(StatusCodes.NotFound -> ErrorResponse("Endpoint not found"))
    )
  }
}

