/**
 * Health Check Service
 * ====================
 * Comprehensive health checking for all system components.
 * Provides detailed status information for monitoring and alerting.
 *
 * Author: Harsh Jain
 */

package server

import llm.LLMProvider
import agent.ConversationAgent
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import java.time.Instant

case class ComponentHealth(
  name: String,
  status: String,  // "healthy", "degraded", "unhealthy"
  message: String,
  lastChecked: String,
  responseTime: Option[Long] = None  // in milliseconds
)

case class SystemHealth(
  status: String,  // "healthy", "degraded", "unhealthy"
  timestamp: String,
  uptime: Long,  // in seconds
  components: List[ComponentHealth],
  version: String = "1.0.0"
)

class HealthCheckService(
  llmProvider: LLMProvider,
  agent: ConversationAgent
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger("HealthCheckService")
  private val startTime = Instant.now()
  
  /**
   * Perform comprehensive health check on all components
   */
  def checkHealth(): Future[SystemHealth] = {
    val timestamp = Instant.now().toString
    
    for {
      llmHealth <- checkLLMProvider()
      agentHealth <- checkAgent()
      memoryHealth <- checkMemory()
    } yield {
      val components = List(llmHealth, agentHealth, memoryHealth)
      val overallStatus = determineOverallStatus(components)
      
      SystemHealth(
        status = overallStatus,
        timestamp = timestamp,
        uptime = getUptimeSeconds(),
        components = components
      )
    }
  }
  
  /**
   * Simple health check (legacy endpoint)
   */
  def simpleHealthCheck(): Future[Boolean] = {
    checkHealth().map(_.status == "healthy").recover {
      case _ => false
    }
  }
  
  /**
   * Check LLM provider health
   */
  private def checkLLMProvider(): Future[ComponentHealth] = {
    val startTime = System.currentTimeMillis()
    
    llmProvider.healthCheck().map { isHealthy =>
      val responseTime = System.currentTimeMillis() - startTime
      ComponentHealth(
        name = "LLM Provider",
        status = if (isHealthy) "healthy" else "unhealthy",
        message = if (isHealthy) "LLM provider is responding" else "LLM provider is not responding",
        lastChecked = Instant.now().toString,
        responseTime = Some(responseTime)
      )
    }.recover {
      case ex =>
        val responseTime = System.currentTimeMillis() - startTime
        ComponentHealth(
          name = "LLM Provider",
          status = "unhealthy",
          message = s"Health check failed: ${ex.getMessage}",
          lastChecked = Instant.now().toString,
          responseTime = Some(responseTime)
        )
    }
  }
  
  /**
   * Check conversation agent health
   */
  private def checkAgent(): Future[ComponentHealth] = Future {
    try {
      val stats = agent.getConversationStats()
      val conversationCount = stats.getOrElse("totalConversations", 0).asInstanceOf[Int]
      
      // Consider degraded if too many active conversations
      val status = if (conversationCount < 1000) "healthy" 
                  else if (conversationCount < 5000) "degraded"
                  else "unhealthy"
      
      ComponentHealth(
        name = "Conversation Agent",
        status = status,
        message = s"Managing $conversationCount active conversations",
        lastChecked = Instant.now().toString
      )
    } catch {
      case ex: Exception =>
        ComponentHealth(
          name = "Conversation Agent",
          status = "unhealthy",
          message = s"Error checking agent: ${ex.getMessage}",
          lastChecked = Instant.now().toString
        )
    }
  }
  
  /**
   * Check memory health
   */
  private def checkMemory(): Future[ComponentHealth] = Future {
    val runtime = Runtime.getRuntime
    val maxMemory = runtime.maxMemory()
    val totalMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    val usedMemory = totalMemory - freeMemory
    val usagePercent = (usedMemory.toDouble / maxMemory * 100).toInt
    
    val status = if (usagePercent < 70) "healthy"
                else if (usagePercent < 85) "degraded"
                else "unhealthy"
    
    ComponentHealth(
      name = "Memory",
      status = status,
      message = s"Memory usage: $usagePercent% (${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB)",
      lastChecked = Instant.now().toString
    )
  }
  
  /**
   * Determine overall system status based on component statuses
   */
  private def determineOverallStatus(components: List[ComponentHealth]): String = {
    if (components.exists(_.status == "unhealthy")) "unhealthy"
    else if (components.exists(_.status == "degraded")) "degraded"
    else "healthy"
  }
  
  /**
   * Get system uptime in seconds
   */
  private def getUptimeSeconds(): Long = {
    Instant.now().getEpochSecond - startTime.getEpochSecond
  }
  
  /**
   * Format uptime for human readability
   */
  def getFormattedUptime(): String = {
    val seconds = getUptimeSeconds()
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    if (days > 0) s"${days}d ${hours}h ${minutes}m ${secs}s"
    else if (hours > 0) s"${hours}h ${minutes}m ${secs}s"
    else if (minutes > 0) s"${minutes}m ${secs}s"
    else s"${secs}s"
  }
}
