fun main() {
    var videoFile = "/home/neitirite/Документы/Kotlin-Video-Backend/spokoynaya_noch"
    val sourceRes = Pair(1920, 1080)
    val ID = "first"
//    startConversion(videoFile, sourceRes, ID)
    APIIntegration().startAPI()
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
            converter().convert(videoFile, label, dims.first, dims.second, id)
        }
    }
}
fun parseData(){

}