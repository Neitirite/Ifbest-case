import kotlin.concurrent.thread

class QueueManager {
    var queue: MutableList<String> = mutableListOf()
    val videoDirectory = "/video_files/source"
    var semaphore = false
    fun addToQueue(id: String, width: Int, height: Int) {
        val qObject = "${id}_${width}_${height}"
        queue.add(qObject)
        println("Added new object")
        if (!semaphore) {
//            manager()
            semaphore = true
        }
    }
    fun removeFromQueue() {
        queue.remove(queue.first())
    }
    fun manager(){
        println("QueueManager running...")
        while (queue.isNotEmpty()) {
            println(queue)
            if (queue.isNotEmpty()) {
                val meta = queue.first().split("_")
                val id = meta[0]
                val width = meta[1].toInt()
                val height = meta[2].toInt()
                println("Starting conversion")
                thread{Converter().convert("${videoDirectory}/${id}", Pair(width, height), id)}
            }
        }
        semaphore = false
    }
}
