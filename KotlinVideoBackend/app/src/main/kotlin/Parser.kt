import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
@Serializable
data class Info(val width: Int, val height: Int, val id: String)

@Serializable
data class Data(val Info: Info)

class Parser {
    fun parse(data: String): String {
        val parsedData = Json.decodeFromString<Data>(data)
        println("Width: ${parsedData.Info.width}")
        println("Height: ${parsedData.Info.height}")
        println("ID: ${parsedData.Info.id}")
        val outData = "${parsedData.Info.id}|${parsedData.Info.width}|${parsedData.Info.height}"
        return outData
    }
}