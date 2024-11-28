import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import io.grpc.{Server, ServerBuilder}
import proto.prompt.{ChatGrpc, PromptRequest, PromptResponse}

import scala.concurrent.{ExecutionContext, Future}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

import play.api.libs.json.{JsValue, Json}

object GrpcServer {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val appConf: Config = ConfigFactory.load().resolve()

  val defaultPort = appConf.getInt("defaultGrpcPort")
  val awsLambdaUrl = appConf.getString("awsLambdaUrl")

  def main(args: Array[String]): Unit = {

    val port = if (args.length < 1) defaultPort else args(0).toInt

    val server = new GrpcServer(ExecutionContext.global, port)
    server.start()
    server.blockUntilShutdown()
  }

}

class GrpcServer(executionContext: ExecutionContext, port: Int) { self =>
  val server: Server = ServerBuilder.forPort(port).addService(ChatGrpc.bindService(new ChatImpl, executionContext)).asInstanceOf[ServerBuilder[_]].build()

  private def start(): Unit = {
    server.start()
    GrpcServer.logger.info("Server started, listening on " + port)
    sys.addShutdownHook {
      GrpcServer.logger.error("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      GrpcServer.logger.error("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class ChatImpl extends ChatGrpc.Chat {

    def prepareRequest(prompt: String): HttpPost = {
      val httpPost = new HttpPost(GrpcServer.awsLambdaUrl)
      val entity = new StringEntity(s"""{"prompt":"$prompt"}""")
      httpPost.setEntity(entity)
      httpPost.setHeader("Accept", "application/json")
      httpPost.setHeader("Content-type", "application/json")
      httpPost
    }

    override def sendPrompt(request: PromptRequest): Future[PromptResponse] = {
      GrpcServer.logger.info(s"Request from FinchServer. Sending to Aws Lambda prompt: ${request.prompt}")
      // prepare request
      val httpPost = prepareRequest(request.prompt)

      // send to awsLambdaUrl
      val client = HttpClients.createDefault()
      try {
        // get response
        val response = client.execute(httpPost)
        val body = EntityUtils.toString(response.getEntity())
        GrpcServer.logger.info(s"Response from Aws Lambda: $body")
        val data = (Json.parse(body) \ "response").get.toString()
        println("hey")
        println(data)
        Future.successful(PromptResponse(response = data.substring(1, data.length() - 1)))
//        Future.successful(PromptResponse(response = "\n* \"I Will Survive\" by Gloria Gaynor"))
      } catch {
        case x: Throwable =>
          GrpcServer.logger.info(x.getMessage())
          Future.failed(new Error("Something wrong with the server"))
      } finally {
        client.close()
      }

    }
  }

}