import java.io.File

val qFile = File("/video_files/queue/queue")
val s3qFile = File("/video_files/queue/s3queue")
val videoDirectory = "/video_files/source"
fun main() {
    if(!s3qFile.exists()){
        s3qFile.createNewFile()
    }
    println("QueueManager running...")
    while (true) {
        val queue = qFile.readLines().toMutableList()
        val s3Queue = s3qFile.readLines().toMutableList()
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
                s3Queue.add(id)
                s3qFile.writeText(s3Queue.joinToString("\n"))
            } else {
                println("Error processing $id")
            }
        }
    }
}