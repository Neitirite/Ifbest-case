import java.io.File
class Converter {

    val outputDir = "/video_files"

    fun convert(videoFile: String, resolution: String, width: Int, height: Int, id: String){

        val fileName = File(videoFile).name.split(".")[0]
        val createDirectory = listOf("mkdir", "${outputDir}/${id}")
        ProcessBuilder(createDirectory).start()

        val outputPlaylist = "${outputDir}/${id}/${resolution}p-${id}.m3u8"
        val outputTS = "${outputDir}/${id}/${resolution}p-${id}%d.ts"
        val ffmpegCommand = listOf(
            "ffmpeg",
            "-y",
            "-i", videoFile,
            "-vf", "scale=w=$width:h=$height:force_original_aspect_ratio=decrease",
            "-c:a", "aac",
            "-ar", "48000",
            "-b:a", "128k",
            "-c:v", "libx264",
            "-profile:v", "main",
            "-crf", "20",
            "-g", "48",
            "-keyint_min", "48",
            "-sc_threshold", "0",
            "-hls_time", "5",
            "-hls_segment_filename", outputTS,
            "-hls_playlist_type", "vod",
            outputPlaylist
        )

        println("Start conversion of $resolution")
        try {
            val process = ProcessBuilder(ffmpegCommand)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().lineSequence().forEach { println(it) } //log

            val exitCode = process.waitFor()
            if(exitCode == 0) {
                println("Success!")
            } else {
                println("error with processing $resolution. Error code: $exitCode")
            }
        } catch (e: Exception) {
            println("Error with running ffmpeg for $resolution: ${e.message}")
        }

    }
}