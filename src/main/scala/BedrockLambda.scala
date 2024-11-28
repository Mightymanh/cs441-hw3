import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsValue, Json}
import com.typesafe.config.{Config, ConfigFactory}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.{InvokeModelRequest, InvokeModelResponse}

import scala.jdk.CollectionConverters._

class BedrockLambda extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val appConf: Config = ConfigFactory.load().resolve()

  val modelId = appConf.getString("modelId")

  // assuming prompt begins and ends with " (quotation mark)
  def prepareBedrockRequest(prompt: String): InvokeModelRequest = {
    val bedRockBody = s"""{"prompt":"${prompt}","maxTokens":200,"temperature":0.8,"topP":0.9,"stopSequences":[],"countPenalty":{"scale":0},"presencePenalty":{"scale":0},"frequencyPenalty":{"scale":0}}"""
    val invokeModelRequest: InvokeModelRequest = InvokeModelRequest.builder()
      .modelId(modelId)
      .body(SdkBytes.fromUtf8String(bedRockBody))
      .build()
    invokeModelRequest
  }

  def processBedrockResponse(bedrockResponse: InvokeModelResponse): String = {
    val body = bedrockResponse.body().asUtf8String()
    val response = (Json.parse(body) \ "completions" \ 0 \ "data" \ "text").as[String]
    response
  }

  def goodResponse(response: String): APIGatewayProxyResponseEvent = {
    val responseBody = s"""{"response":"${response}"}"""
    val responseEvent: APIGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent()
    responseEvent.setStatusCode(200)
    responseEvent.withHeaders(Map(
      "Access-Control-Allow-Origin" -> "*",
      "Content-Type" -> "application/json"
    ).asJava)
    responseEvent.setBody(responseBody)
    responseEvent
  }

  def badResponse(): APIGatewayProxyResponseEvent = {
    val responseBody = s"""{"response":"Something wrong with server"}"""
    val responseEvent: APIGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent()
    responseEvent.setStatusCode(500)
    responseEvent.withHeaders(Map(
      "Access-Control-Allow-Origin" -> "*",
      "Content-Type" -> "application/json"
    ).asJava)
    responseEvent.setBody(responseBody)
    responseEvent
  }

  override def handleRequest(i: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    try {
      // extract the query from the request
      val requestBody = i.getBody()
      logger.info(s"requestBody: $requestBody")
      val prompt = (Json.parse(requestBody) \ "prompt").as[String]
      logger.info(s"prompt: $prompt")

      // create bedrock client
      val bedRockClient: BedrockRuntimeClient = BedrockRuntimeClient.builder()
        .region(Region.US_EAST_1)
        .build()

      // send the query to bedrock and get bedrockResponse
      val bedrockRequest = prepareBedrockRequest(prompt)
      val bedrockResponse = bedRockClient.invokeModel(bedrockRequest)

      // process bedrockResponse and return response
      val bedrockResData = processBedrockResponse(bedrockResponse)
      logger.info(s"response: $bedrockResData")
      bedRockClient.close()
      goodResponse(bedrockResData)
    } catch {
      case x: Throwable => {
        logger.error(x.getMessage)
        badResponse()
      }
    }
  }
}
