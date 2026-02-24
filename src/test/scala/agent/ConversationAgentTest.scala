package agent

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import llm.{LLMProvider, ConversationContext, ConversationMessage}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class MockLLMProvider extends LLMProvider {
  override def generateResponse(prompt: String): Future[String] = {
    Future.successful(s"Mock response to: $prompt")
  }

  override def healthCheck(): Future[Boolean] = {
    Future.successful(true)
  }
}

class ConversationAgentTest extends AnyFlatSpec with Matchers {
  implicit val ec: ExecutionContext = global
  
  val agent = new ConversationAgent(new MockLLMProvider())

  "ConversationAgent" should "process a message and generate a response" in {
    val conversationId = "test-conv-1"
    val message = "Hello, how are you?"
    
    val responseFuture = agent.processMessage(conversationId, message)
    
    val response = scala.concurrent.Await.result(responseFuture, scala.concurrent.duration.Duration(5, "seconds"))
    response should not be empty
    response should contain("Mock response")
  }

  it should "maintain conversation history" in {
    val conversationId = "test-conv-2"
    
    scala.concurrent.Await.result(agent.processMessage(conversationId, "First message"), scala.concurrent.duration.Duration(5, "seconds"))
    scala.concurrent.Await.result(agent.processMessage(conversationId, "Second message"), scala.concurrent.duration.Duration(5, "seconds"))
    
    val context = agent.getConversation(conversationId)
    context should not be empty
    context.get.messages.length should be(4) // 2 user + 2 assistant
  }

  it should "return conversation statistics" in {
    val stats = agent.getConversationStats()
    
    stats should contain key "totalConversations"
    stats should contain key "totalMessages"
    stats should contain key "avgMessagesPerConversation"
  }

  it should "clear conversation history" in {
    val conversationId = "test-conv-3"
    
    scala.concurrent.Await.result(agent.processMessage(conversationId, "Test message"), scala.concurrent.duration.Duration(5, "seconds"))
    
    val cleared = agent.clearConversation(conversationId)
    cleared should be(true)
    
    agent.getConversation(conversationId) should be(None)
  }
}
