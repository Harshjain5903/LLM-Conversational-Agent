package agent

import llm.{LLMProvider, ConversationContext, ConversationMessage}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable

class ConversationAgent(llmProvider: LLMProvider)(implicit ec: ExecutionContext) {
  private val logger = Logger("ConversationAgent")
  private val conversationContexts = mutable.Map[String, ConversationContext]()

  /**
   * Process a user message and generate an agent response
   */
  def processMessage(conversationId: String, userMessage: String): Future[String] = {
    logger.debug(s"Processing message for conversation: $conversationId")
    
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
}
