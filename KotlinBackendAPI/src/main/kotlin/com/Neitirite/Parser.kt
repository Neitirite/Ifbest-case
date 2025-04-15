package com.Neitirite

import com.google.gson.Gson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Info(val width: Int, val height: Int, val id: String)

@Serializable
data class Data(val Info: Info)

class Parser {
    fun parse(data: String): Data {
        val parsedData = Json.decodeFromString<Data>(data)
        println("Width: ${parsedData.Info.width}")
        println("Height: ${parsedData.Info.height}")
        println("ID: ${parsedData.Info.id}")
        return parsedData
    }
    val qFile = File("/video_files/queue/queue")
    fun getQueue(): String{
        var queue = qFile.readLines().toMutableList()
        val resultMap = mutableMapOf<String, String>()
        if(queue.isNotEmpty()) {
            queue.forEach { obj ->
                val splitObj = obj.split("_")
                resultMap[splitObj[0]] = "${splitObj[1]}x${splitObj[2]}"
            }
        }
        val jsonOutput = Gson().toJson(resultMap)
        return jsonOutput
    }
}