var res: String? = null
fun main() {

    APIIntegration().startAPI()
}



fun processApiData(data: String){
    res = Parser().parse(data)
}

fun startConversion(videoFile: String, sourceRes: Pair<Int, Int>, id: String){
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
    resolutions.forEach{ (label, dims) ->
        if(dims.first <= sourceRes.first && dims.second <= sourceRes.second){
            Converter().convert(videoFile, label, dims.first, dims.second, id)
        }
    }
}

