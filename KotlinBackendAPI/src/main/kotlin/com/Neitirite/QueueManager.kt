package com.Neitirite

import java.io.File
val qFile = File("/video_files/queue/queue")

class QueueManager {

    var queue: MutableList<String> = mutableListOf()


    fun addToQueue(id: String, width: Int, height: Int) {
        if (!qFile.exists()) {
            qFile.createNewFile()
        }
        queue = qFile.readLines().toMutableList()
        val qObject = "${id}_${width}_${height}"
        queue.add(qObject)
        println(queue)
        qFile.writeText(queue.joinToString("\n"))
        println("Added new object")
    }
}
