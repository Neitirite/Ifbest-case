package org.example
import Converter
import java.io.File

var queue: MutableList<String> = mutableListOf()
val qFile = File("/video_files/queue/queue")
val videoDirectory = "/video_files/source"
fun main() {

    println("QueueManager running...")
    while (true) {
        queue = qFile.readLines().toMutableList()
        if (queue.isNotEmpty()) {
            println(queue)
            val meta = queue.first().split("_")
            val id = meta[0]
            val width = meta[1].toInt()
            val height = meta[2].toInt()
            println("Starting conversion")
            val conv = Converter().convert("${videoDirectory}/${id}", Pair(width, height), id)
            if(conv == 0){
                queue.remove(queue.first())
                qFile.writeText(queue.joinToString("\n"))
            } else {
                println("Error processing ${id}")
            }
        }
    }
}