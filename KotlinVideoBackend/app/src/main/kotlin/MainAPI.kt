import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import java.io.File
import kotlin.time.Duration.Companion.seconds


fun main() {
    println("Starting Websocket server")
    embeddedServer(Netty, port = 2077){
        install(WebSockets){
            pingPeriod = 15.seconds
            timeout = 30.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        println("Websocket server started")
        routing {
            webSocket ("/api"){
                println("New connection: ${this.call.request.origin.remoteHost}")
                val binaryChunks = mutableListOf<ByteArray>()
                var id: String? = null
                var width: Int? = null
                var height: Int? = null
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                if (text == "EOF"){
                                    println("EOF received")
                                    break
                                } else if (text == "getQueue") {
                                    val q = Parser().getQueue()
                                    send(q)
                                } else {
                                    val parsedText = Parser().parse(text)
                                    id = parsedText.Info.id
                                    width = parsedText.Info.width
                                    height = parsedText.Info.height
//                                    println("JSON: $text")
                                }
                            }
                            is Frame.Binary -> {
                                val chunk = frame.data
                                binaryChunks.add(chunk)
//                                    println("Received ${chunk.size} bytes chunk")
                            }
                            else -> {
                                continue
                            }
                        }
                    }
                    if (binaryChunks.isNotEmpty()) {
                        println("Exporting video")
                        val allBytes = binaryChunks.fold(ByteArray(0)) {acc, bytes -> acc + bytes}
                        withContext(Dispatchers.IO) {
                            if (id != null && width != null && height != null) {
                                val outVideo = File("/video_files/source/${id}")
                                outVideo.writeBytes(allBytes)
                                println("Success! Adding to queue")
                                thread(name = "converter"){QueueManager().addToQueue(id, width, height)}
                            } else {
                                println("Error: No metadata found!")
                            }
                        }


                    }

                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }

        }
    }.start(wait = true)
}
