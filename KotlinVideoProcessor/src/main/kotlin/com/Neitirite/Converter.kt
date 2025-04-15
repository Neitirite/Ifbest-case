package com.Neitirite

import java.io.File
class Converter {

    val outputDir = "/video_files"
    val resolutions = mapOf(
        "4K" to Pair(3840, 2160),
        "2K" to Pair(2560, 1440),
        "1080" to Pair(1920, 1080),
        "720" to Pair(1280, 720),
        "480" to Pair(854, 480),
        "360" to Pair(640, 360),
        "240" to Pair(426, 240),
        "144" to Pair(256, 144)
    )

    fun convert(videoFile: String, res: Pair<Int, Int>, id: String): Int {

        val fileName = File(videoFile).name.split(".")[0]
        val createDirectory = listOf("mkdir", "${outputDir}/${id}")
        ProcessBuilder(createDirectory).start()



        resolutions.forEach { (label, dims) ->
            val outputPlaylist = "${outputDir}/${id}/${label}p-${id}.m3u8"
            val outputTS = "${outputDir}/${id}/${label}p-${id}%d.ts"
            val (width, height) = dims
            val ffmpegCommand = listOf(
                "ffmpeg",
                "-v", "quiet",
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

            if (dims.first <= res.first && dims.second <= res.second) {
                println("Start conversion of ${id}_${label}")
                try {
                    val process = ProcessBuilder(ffmpegCommand)
                        .redirectErrorStream(true)
                        .start()

                    process.inputStream.bufferedReader().lineSequence().forEach { println(it) } //log

                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        println("Success!")
                    } else {
                        println("error with processing $label. Error code: $exitCode")
                    }
                } catch (e: Exception) {
                    println("Error with running ffmpeg for $label: ${e.message}")
                }
            }
        }
        return 0
    }
}