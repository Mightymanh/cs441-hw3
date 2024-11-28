import com.twitter.finagle.{Http, ListeningServer}
import com.twitter.util.Await
import com.typesafe.config.{Config, ConfigFactory}
import io.finch._
import io.finch.circe._
import io.finch.syntax._
import io.circe.generic.auto._
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.TimeUnit
import proto.prompt.{ChatGrpc, PromptRequest}
import proto.prompt.ChatGrpc.ChatBlockingStub
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}

case class Prompt(prompt: String)
case class Response(response: String)

object FinchServer {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val appConf: Config = ConfigFactory.load().resolve()

  val defaultFinchPort: Int = appConf.getInt("defaultFinchPort")
  val defaultGrpcPort: Int = appConf.getInt("defaultGrpcPort")
  val defaultGrpcHost: String = appConf.getString("defaultGrpcHost")

  def main(args: Array[String]): Unit = {
    val finchPort: Int = if (args.length < 1) defaultFinchPort else args(0).toInt
    val grpcPort: Int = if (args.length < 2) defaultGrpcPort else args(1).toInt
    val grpcHost: String = if (args.length < 3) defaultGrpcHost else args(2)

    val grpcClient = new GrpcClient(grpcHost, grpcPort)

    try {
      // endpoint GET /hello
      val hello: Endpoint[String] = get("hello") {
        logger.info("get hello")
        Ok("Hello, I am Finch Server!").withHeaders(Map(
          "Access-Control-Allow-Origin" -> "*"
        ))
      }

      // endpoint POST /prompt
      val prompt: Endpoint[Response] = post("prompt" :: jsonBody[Prompt]) { prompt: Prompt =>
        logger.info("post prompt")
        try {
          val response = grpcClient.requestGrpc(prompt.prompt) // in the form {"response":"<response>"}
          logger.info(response.response)
          Ok(response).withHeaders(Map(
            "Access-Control-Allow-Origin" -> "*"
          ))
        } catch {
          case x: Throwable => {
            logger.error(x.getMessage)
            InternalServerError(new Exception("Something wrong with server")).withHeaders(Map(
              "Access-Control-Allow-Origin" -> "*"
            ))
          }
        }
      }

      // server
      val server = Http.server.serve(s":$finchPort", (hello :+: prompt).toService)
      logger.info(s"Finch Server is listening on port $finchPort. Grpc Server should be at $grpcHost on port $grpcPort")
      Await.ready(server)
    } catch {
      case x: Throwable => logger.error(x.getMessage)
    } finally {
      grpcClient.shutdown()
      logger.warn(s"Finch Server shuts down grpc communication with Grpc Server $grpcHost:$grpcPort")
    }
  }
}

class GrpcClient(host: String, port: Int) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().asInstanceOf[ManagedChannelBuilder[_]].build()
  val blockingStub: ChatBlockingStub = ChatGrpc.blockingStub(channel)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def requestGrpc(prompt: String): Response = {

    logger.info(s"Try to send to Grpc Server the prompt: ${prompt}")

    val request = PromptRequest(prompt)
    try {
      val response = blockingStub.sendPrompt(request)
      val data = response.response
      logger.info("Grpc Server replies: " + data)
      Response(response = data)
    } catch {
      case e: StatusRuntimeException =>
        logger.error(s"RPC failed: ${e.getStatus}")
        throw new scala.Error("RPC failure")
    }
  }

}