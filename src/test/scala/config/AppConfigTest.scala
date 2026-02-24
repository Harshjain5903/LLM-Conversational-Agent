package config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppConfigTest extends AnyFlatSpec with Matchers {
  "AppConfig" should "load configuration successfully" in {
    val config = AppConfig.load()
    
    config.server.host should equal("0.0.0.0")
    config.server.port should equal(8080)
  }

  it should "have valid LLM backend" in {
    val config = AppConfig.load()
    
    val validBackends = List("bedrock", "ollama")
    validBackends should contain(config.llm.backend)
  }

  it should "have Bedrock configuration" in {
    val config = AppConfig.load()
    
    config.llm.bedrock.region should not be empty
    config.llm.bedrock.model should not be empty
  }

  it should "have Ollama configuration" in {
    val config = AppConfig.load()
    
    config.llm.ollama.endpoint should not be empty
    config.llm.ollama.model should not be empty
  }
}
