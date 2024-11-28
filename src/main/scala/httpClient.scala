import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

object httpClient {

  val url = "https://bwlaz3zizk.execute-api.us-east-2.amazonaws.com/bedrock-function"

  def main(args: Array[String]): Unit = {
    val httpPost = new HttpPost(url)
    val json = s"""{"prompt":"Who are you?"}"""
    val entity = new StringEntity(json)
    httpPost.setEntity(entity)
    httpPost.setHeader("Accept", "application/json")
    httpPost.setHeader("Content-type", "application/json")

    val client = HttpClients.createDefault()
    try {
      val response = client.execute(httpPost)
      val body = EntityUtils.toString(response.getEntity())
      println(body)
    } finally {
      client.close()
    }

  }
}
