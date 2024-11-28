import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import play.api.libs.json.{JsValue, Json}

object BedRockRun {
  val modelId = "ai21.j2-mid-v1"
  val prompt = "Hello! My name is Manh! What do you think about Sherlock Holmes?"

  def main(args: Array[String]): Unit = {
    // create bedrock client
    val bedRockClient: BedrockRuntimeClient = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .credentialsProvider(ProfileCredentialsProvider.create())
      .build()

    try {

      val bedRockBody = s"""{"prompt":"${prompt}","maxTokens":200,"temperature":0.8,"topP":0.9,"stopSequences":[],"countPenalty":{"scale":0},"presencePenalty":{"scale":0},"frequencyPenalty":{"scale":0}}"""

      println(s"request body: $bedRockBody")

      // create request
      val invokeModelRequest: InvokeModelRequest = InvokeModelRequest.builder()
        .modelId(modelId)
        .body(SdkBytes.fromUtf8String(bedRockBody))
        .build()

      // send request, retrieve response
      val invokeModelResponse = bedRockClient.invokeModel(invokeModelRequest)
      val responseBody = invokeModelResponse.body().asUtf8String()
      println(s"response: $responseBody")
      val json: JsValue = Json.parse(responseBody)
      val data = (json \ "completions" \ 0 \ "data" \ "text").as[String]
      println(s"data: ${data}")

    } catch {
      case error: Throwable => println(error)
    } finally {
      if (bedRockClient != null) bedRockClient.close()
    }
  }
}
