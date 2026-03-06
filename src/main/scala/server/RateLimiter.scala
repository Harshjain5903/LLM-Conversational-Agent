/**
 * Rate Limiter
 * =============
 * Token bucket-based rate limiter for API endpoints.
 * Prevents abuse and ensures fair usage of resources.
 *
 * Author: Harsh Jain
 */

package server

import scala.collection.mutable
import java.time.Instant

class RateLimiter(maxRequests: Int, windowSeconds: Int) {
  
  private case class TokenBucket(var tokens: Int, var lastRefill: Instant)
  
  private val buckets = mutable.Map[String, TokenBucket]()
  
  /**
   * Check if a request is allowed for the given identifier
   * @param identifier Unique identifier (e.g., conversationId or IP address)
   * @return true if request is allowed, false if rate limit exceeded
   */
  def isAllowed(identifier: String): Boolean = {
    val now = Instant.now()
    
    val bucket = buckets.getOrElseUpdate(identifier, TokenBucket(maxRequests, now))
    
    // Refill tokens based on time elapsed
    val secondsElapsed = now.getEpochSecond - bucket.lastRefill.getEpochSecond
    if (secondsElapsed >= windowSeconds) {
      bucket.tokens = maxRequests
      bucket.lastRefill = now
    } else {
      val tokensToAdd = (secondsElapsed * maxRequests / windowSeconds).toInt
      bucket.tokens = Math.min(maxRequests, bucket.tokens + tokensToAdd)
      if (tokensToAdd > 0) {
        bucket.lastRefill = now
      }
    }
    
    // Consume a token if available
    if (bucket.tokens > 0) {
      bucket.tokens -= 1
      true
    } else {
      false
    }
  }
  
  /**
   * Get remaining tokens for an identifier
   */
  def getRemainingTokens(identifier: String): Int = {
    buckets.get(identifier).map(_.tokens).getOrElse(maxRequests)
  }
  
  /**
   * Clear rate limit for an identifier (for testing or admin purposes)
   */
  def reset(identifier: String): Unit = {
    buckets.remove(identifier)
  }
  
  /**
   * Cleanup old buckets to prevent memory leaks
   */
  def cleanup(): Unit = {
    val now = Instant.now()
    val oldBuckets = buckets.filter { case (_, bucket) =>
      now.getEpochSecond - bucket.lastRefill.getEpochSecond > windowSeconds * 2
    }
    oldBuckets.keys.foreach(buckets.remove)
  }
}

object RateLimiter {
  // Default: 60 requests per minute
  def apply(): RateLimiter = new RateLimiter(60, 60)
  
  def apply(maxRequests: Int, windowSeconds: Int): RateLimiter = 
    new RateLimiter(maxRequests, windowSeconds)
}
