import io.minio.*
import io.minio.http.Method
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*


val configFile = File("${System.getProperty("user.home")}/.config/s3API.conf")

fun loadConfig(configFile: File): Properties {
    val props = Properties()
    FileInputStream(configFile).use { props.load(it) }
    return props
}

// Если какие-то параметры отсутствуют, запрашиваем их ввод
fun promptIfMissing(props: Properties, key: String, promptMessage: String) {
    if (props.getProperty(key).isNullOrBlank()) {
        print(promptMessage)
        val input = readLine()
        if (!input.isNullOrBlank()) {
            props.setProperty(key, input.toString())
        }
    }
}

fun main() {
    val config = Properties()
    val videoDirectory = File("/video_files")
    val s3qFile = File("/video_files/queue/s3queue")
    if(!s3qFile.exists()){
        s3qFile.createNewFile()
    }
    if (configFile.exists() && configFile.isFile) {
        try {
            config.putAll(loadConfig(configFile))
        } catch (e: IOException) {
            println("Ошибка чтения файла конфигурации: ${e.message}")
        }
    } else {
        println("Файл конфигурации ${configFile.absolutePath} не найден. Введите параметры вручную.")
    }

    promptIfMissing(config, "endpoint", "endpoint: (http://ip:port) ")
    promptIfMissing(config, "accessKey", "accessKey: ")
    promptIfMissing(config, "secretKey", "secretKey: ")
    promptIfMissing(config, "bucketName", "bucketName: ")

    val endpoint = config.getProperty("endpoint")
    val accessKey = config.getProperty("accessKey")
    val secretKey = config.getProperty("secretKey")
    val bucketName = config.getProperty("bucketName")

    val minioClient = MinioClient.builder()
        .endpoint(endpoint.split(":").first(), endpoint.split(":").last().toInt(), false)
        .credentials(accessKey, secretKey)
        .build()

    if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
        println("Bucket '$bucketName' создан")
    }
    while (true) {
        val s3Queue = s3qFile.readLines().toMutableList()
        if (s3Queue.isNotEmpty()) {
            println(s3Queue.joinToString("\n"))
            val directory = File("${videoDirectory}/${s3Queue.first()}")
            if(!directory.exists()) {
                println("Directory does not exist: ${directory.absolutePath}")
                s3Queue.removeFirst(); s3qFile.writeText(s3Queue.joinToString("\n"))
            } else {
                directory.walk().filter { it.isFile }.forEach { file ->
                    val objectName = file.relativeTo(directory).invariantSeparatorsPath

                    FileInputStream(file).use { stream ->
                        minioClient.putObject(
                            PutObjectArgs.builder()
                                .bucket(bucketName)
                                .`object`(objectName)
                                .stream(stream, file.length(), -1)
                                .build()
                        )
                    }
                    if(objectName.endsWith(".m3u8")) {
                        val linkToObject = minioClient.getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .`object`(objectName)
                                .build()
                        )
                        println(linkToObject)
                       //Сделать отправку на бэкенд id и ссылки на файл в S3
                    }

                }
                s3Queue.removeFirst()
                s3qFile.writeText(s3Queue.joinToString("\n"))
            }
            directory.deleteRecursively()

        }
    }
}