import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.seconds
import java.util.UUID
class APIIntegration {
    fun startAPI(){

        embeddedServer(Netty, port = 2077){
            install(WebSockets){
                pingPeriod = 15.seconds
                timeout = 30.seconds
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket ("/api"){
                    println("New connection: ${this.call.request.origin.remoteHost}")
                    val binaryChunks = mutableListOf<ByteArray>()
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    if (text == "EOF"){
                                        println("EOF received")
                                        break
                                    }
                                    processApiData(text)
//                                    println("JSON: $text")
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
                                val outVideo = File("/video_files/source/${UUID.randomUUID()}")
                                outVideo.writeBytes(allBytes)

                            }
                            println("Success!")

                        }

                    } catch (e: Exception) {
                        println("Error: ${e.message}")
                    }
                }

            }
        }.start(wait = true)

    }

}