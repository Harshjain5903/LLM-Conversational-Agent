/**
 * Request Validator
 * ==================
 * Validates incoming HTTP requests to ensure data integrity and security.
 * Provides validation rules for chat messages and conversation IDs.
 *
 * Author: Harsh Jain
 */

package server

import scala.util.matching.Regex

object RequestValidator {
  
  private val conversationIdPattern: Regex = "^[a-zA-Z0-9_-]{1,100}$".r
  private val maxMessageLength: Int = 10000
  private val minMessageLength: Int = 1
  
  /**
   * Validation result
   */
  sealed trait ValidationResult
  case object Valid extends ValidationResult
  case class Invalid(reason: String) extends ValidationResult
  
  /**
   * Validate a chat request
   */
  def validateChatRequest(message: String, conversationId: String): ValidationResult = {
    validateMessage(message) match {
      case Valid => validateConversationId(conversationId)
      case invalid => invalid
    }
  }
  
  /**
   * Validate message content
   */
  def validateMessage(message: String): ValidationResult = {
    if (message == null || message.isBlank) {
      Invalid("Message cannot be empty or null")
    } else if (message.length < minMessageLength) {
      Invalid(s"Message must be at least $minMessageLength character(s)")
    } else if (message.length > maxMessageLength) {
      Invalid(s"Message exceeds maximum length of $maxMessageLength characters")
    } else {
      Valid
    }
  }
  
  /**
   * Validate conversation ID format
   */
  def validateConversationId(conversationId: String): ValidationResult = {
    if (conversationId == null || conversationId.isBlank) {
      Invalid("Conversation ID cannot be empty or null")
    } else if (!conversationIdPattern.matches(conversationId)) {
      Invalid("Conversation ID must contain only alphanumeric characters, underscores, and hyphens (max 100 chars)")
    } else {
      Valid
    }
  }
  
  /**
   * Sanitize user input to prevent injection attacks
   */
  def sanitizeInput(input: String): String = {
    if (input == null) return ""
    
    // Remove potentially dangerous characters while preserving normal text
    input
      .replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F]", "") // Remove control characters
      .trim
  }
}
