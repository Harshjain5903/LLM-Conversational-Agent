/**
 * Conversation Agent
 * ==================
 * Orchestrates multi-turn conversations with context management.
 * Handles message processing, history tracking, and conversation lifecycle.
 *
 * Author: Harsh Jain
 */

package agent

import llm.{LLMProvider, ConversationContext, ConversationMessage}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable
import java.time.Instant
import scala.concurrent.duration._

class ConversationAgent(llmProvider: LLMProvider)(implicit ec: ExecutionContext) {
  private val logger = Logger("ConversationAgent")
  private val conversationContexts = mutable.Map[String, ConversationContext]()
  private val lastAccessTimes = mutable.Map[String, Instant]()
  
  // TTL configuration: conversations inactive for 1 hour will be cleaned up
  private val conversationTTL: Duration = 1.hour
  private val cleanupInterval: Duration = 15.minutes
  
  // Schedule periodic cleanup
  scheduleCleanup()

  /**
   * Process a user message and generate an agent response
   */
  def processMessage(conversationId: String, userMessage: String): Future[String] = {
    logger.debug(s"Processing message for conversation: $conversationId")
    
    // Update last access time
    lastAccessTimes(conversationId) = Instant.now()
    
    try {
      // Get or create conversation context
      val context = conversationContexts.getOrElseUpdate(
        conversationId,
        ConversationContext(conversationId = conversationId)
      )

      // Add user message to history
      val updatedContext = context.copy(
        messages = context.messages :+ ConversationMessage(role = "user", content = userMessage)
      )
      conversationContexts(conversationId) = updatedContext

      // Build prompt from conversation history
      val prompt = buildPrompt(updatedContext)

      // Generate response using LLM
      llmProvider.generateResponse(prompt).map { response =>
        val assistantMessage = ConversationMessage(role = "assistant", content = response)
        
        // Update context with assistant response
        val finalContext = updatedContext.copy(
          messages = updatedContext.messages :+ assistantMessage
        )
        conversationContexts(conversationId) = finalContext

        logger.debug(s"Response generated for conversation: $conversationId")
        response
      }.recoverWith {
        case ex =>
          logger.error(s"Error generating response: ${ex.getMessage}", ex)
          Future.failed(ex)
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Error processing message: ${ex.getMessage}", ex)
        Future.failed(ex)
    }
  }

  /**
   * Get conversation history
   */
  def getConversation(conversationId: String): Option[ConversationContext] = {
    conversationContexts.get(conversationId)
  }

  /**
   * Clear conversation history
   */
  def clearConversation(conversationId: String): Boolean = {
    conversationContexts.remove(conversationId).isDefined
  }

  /**
   * Build a prompt from conversation history
   */
  private def buildPrompt(context: ConversationContext): String = {
    val conversationText = context.messages
      .map(msg => s"${msg.role}: ${msg.content}")
      .mkString("\n")

    s"""You are a helpful conversational AI assistant. 
       |Conversation history:
       |$conversationText
       |
       |assistant:""".stripMargin
  }

  /**
   * Get conversation statistics
   */
  def getConversationStats(): Map[String, Any] = {
    val totalConversations = conversationContexts.size
    val totalMessages = conversationContexts.values.map(_.messages.length).sum
    
    Map(
      "totalConversations" -> totalConversations,
      "totalMessages" -> totalMessages,
      "avgMessagesPerConversation" -> 
        if (totalConversations > 0) totalMessages / totalConversations else 0
    )
  }
  
  /**
   * Clean up inactive conversations based on TTL
   */
  private def cleanupInactiveConversations(): Unit = {
    val now = Instant.now()
    val ttlSeconds = conversationTTL.toSeconds
    
    val inactiveConversations = lastAccessTimes.filter { case (_, lastAccess) =>
      now.getEpochSecond - lastAccess.getEpochSecond > ttlSeconds
    }.keys.toList
    
    if (inactiveConversations.nonEmpty) {
      inactiveConversations.foreach { conversationId =>
        conversationContexts.remove(conversationId)
        lastAccessTimes.remove(conversationId)
      }
      logger.info(s"Cleaned up ${inactiveConversations.size} inactive conversations (TTL: ${conversationTTL})")
    }
  }
  
  /**
   * Schedule periodic cleanup task
   */
  private def scheduleCleanup(): Unit = {
    import scala.concurrent.duration._
    
    val cancellable = ec match {
      case dispatcher: akka.actor.typed.DispatcherSelector =>
        // For production with Akka scheduler
        logger.info(s"Scheduling conversation cleanup every $cleanupInterval")
        akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem(
          akka.actor.typed.ActorSystem(akka.actor.typed.Behavior.empty, "cleanup-scheduler")
        ).scheduleAtFixedRate(cleanupInterval, cleanupInterval)(() => cleanupInactiveConversations())
      case _ =>
        // Fallback for other execution contexts
        logger.info(s"Scheduling conversation cleanup using simple scheduler (every $cleanupInterval)")
        val timer = new java.util.Timer(true)
        timer.scheduleAtFixedRate(
          new java.util.TimerTask {
            def run(): Unit = cleanupInactiveConversations()
          },
          cleanupInterval.toMillis,
          cleanupInterval.toMillis
        )
        null
    }
  }
  
  /**
   * Manual cleanup trigger for testing or administrative purposes
   */
  def triggerCleanup(): Int = {
    val beforeSize = conversationContexts.size
    cleanupInactiveConversations()
    val afterSize = conversationContexts.size
    beforeSize - afterSize
  }
}
