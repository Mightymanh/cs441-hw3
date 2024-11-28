import play.api.libs.json.{JsValue, Json}

object JsonPlay {
  def main(args: Array[String]): Unit = {
    val prompt = "hey"
    val body = s"""{"prompt": "$prompt"}"""
    val json: JsValue = Json.parse(body)
    val data = (json \ "prompt").get.toString()
    val data2 = data.substring(1, data.length - 1)
    println(data2)
  }

}
