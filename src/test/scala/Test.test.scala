import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import play.api.libs.json.Json

import java.lang.Thread.State
import scala.concurrent.ExecutionContext

class Test extends munit.FunSuite {

  // finch server
  val finchThread = new FinchThread()
  val thread = new Thread(finchThread)
  thread.setName("Finch Server thread")


  // start finch server
  override def beforeAll() {
    // starts the server and let the server takes time to start
    thread.start()
    val startTime = System.currentTimeMillis()
    println("Starting Finch server. If there is a finch thread that is running at port 8080, then ignore current thread")
    // I have to use while loop because I want the server to finish starting before we can allow test that interacts with
    // the server to executes.
    while (thread.getState != State.TIMED_WAITING && (System.currentTimeMillis() - startTime <= 4000)) {
      // do nothing
    }
  }

  test("BedrockLambda - GoodResponse") {
    val response = "I like music"
    val responseEvent = new BedrockLambda().goodResponse(response)
    assertEquals(responseEvent.getStatusCode.toInt, 200, "Status code should be 200 for good response")
    val actualResponse = ((Json.parse(responseEvent.getBody)) \ "response").as[String]
    assertEquals(response, actualResponse, "the ResponseEvent should have its body = input")
  }

  test("BedrockLambda - BadResponse") {
    val responseEvent = new BedrockLambda().badResponse()
    assertEquals(responseEvent.getStatusCode.toInt, 500, "Status code should be 500 for server-error response")
  }

  test("Finch - bad request") {

    // send hello request
    val httpclient = HttpClients.createDefault()
    val httpGet = new HttpGet("http://localhost:8080/notExist")
    val httpResponse = httpclient.execute(httpGet)
    val statusCode = httpResponse.getStatusLine.getStatusCode

    // check
    assertEquals(statusCode, 404, "wrong status code")
  }

  test("Finch - hello request") {

    // send hello request
    val httpclient = HttpClients.createDefault()
    val httpGet = new HttpGet("http://localhost:8080/hello")
    val httpResponse = httpclient.execute(httpGet)
    val statusCode = httpResponse.getStatusLine.getStatusCode
    val body = EntityUtils.toString(httpResponse.getEntity)

    // check
    assertEquals(statusCode, 200, "wrong status code")
    assertEquals(body, "\"Hello, I am Finch Server!\"", "wrong body")
  }

  test("BedrockLambda - prepareBedrockRequest") {
    val prompt = "Hey"
    val expectedBody = s"""{"prompt":"${prompt}","maxTokens":200,"temperature":0.8,"topP":0.9,"stopSequences":[],"countPenalty":{"scale":0},"presencePenalty":{"scale":0},"frequencyPenalty":{"scale":0}}"""
    val invokeRequest = new BedrockLambda().prepareBedrockRequest(prompt)
    val actualBody = invokeRequest.body().asUtf8String()
    assertEquals(expectedBody, actualBody, "invokeRequest has incorrect body")
  }

}

class FinchThread extends Runnable {

  override def run(): Unit = {
    println("Finch thread runs")
    FinchServer.main(Array())
  }
}