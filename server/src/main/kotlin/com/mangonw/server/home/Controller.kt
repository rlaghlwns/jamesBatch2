package com.mangonw.server.home

import com.mangonw.server.mail.WebMailVO
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths

@RestController
@RequestMapping("/")
class Controller {

    @Autowired
    private lateinit var service: Service

    @GetMapping("test")
    fun test() {
        service.test()
    }

    @GetMapping("inline/{yyyy}/{mm}/{fileCode}")
    fun inline(
        @PathVariable yyyy: String,
        @PathVariable mm: String,
        @PathVariable fileCode: String,
    ): ResponseEntity<ByteArray> {
        var inputStream: InputStream? = null
        var entity: ResponseEntity<ByteArray>
        try {
            val searchVo = WebMailVO()
            searchVo.fileCode = fileCode
            searchVo.yyyy = yyyy
            searchVo.mm = mm
            val fileVo: WebMailVO = service.selectFile(searchVo)

            val formatName = fileVo.fileName.substring(fileVo.fileName.lastIndexOf(".") + 1)
            val mType = MediaUtils.getMediaType(formatName)
            val headers = HttpHeaders()
            inputStream = FileInputStream("$FILE_ROOT_PATH$yyyy/$mm/$fileCode")

            headers.contentType = mType
            entity = ResponseEntity<ByteArray>(IOUtils.toByteArray(inputStream), headers, HttpStatus.CREATED)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            entity = ResponseEntity(HttpStatus.BAD_REQUEST)
        } finally {
            inputStream?.close()
        }
        return entity
    }

    @GetMapping("download/{yyyy}/{mm}/{fileCode}@{fileName}")
    fun download(
        @PathVariable yyyy: String,
        @PathVariable mm: String,
        @PathVariable fileCode: String,
        @PathVariable fileName: String,
    ): ResponseEntity<Any> {
        var entity: ResponseEntity<Any>
        try {
            val searchVo = WebMailVO()
            searchVo.fileCode = fileCode
            searchVo.yyyy = yyyy
            searchVo.mm = mm
            val fileVo: WebMailVO = service.selectFile(searchVo)
            val path = "${fileVo.filePath}${fileVo.fileCode}"

            val filePath = Paths.get(path)
            val resource = InputStreamResource(Files.newInputStream(filePath)) // 파일 resource 얻기

            val headers = HttpHeaders()
            headers.contentDisposition = ContentDisposition
                .builder("attachment")
                .filename(fileVo.fileName, charset("UTF-8"))
                .build() // 다운로드 되거나 로컬에 저장되는 용도로 쓰이는지를 알려주는 헤더

            entity = ResponseEntity<Any>(resource, headers, HttpStatus.OK)

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            entity = ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return entity
    }

    companion object {
        private const val FILE_ROOT_PATH = "/usr/local/mailAtcFile/"

        object MediaUtils {
            private var mediaMap: MutableMap<String, MediaType>? = null
            fun getMediaType(type: String): MediaType? {
                return mediaMap!![type.uppercase()]
            }
            init {
                mediaMap = HashMap()
                mediaMap!!["JPG"] = MediaType.IMAGE_JPEG
                mediaMap!!["GIF"] = MediaType.IMAGE_GIF
                mediaMap!!["PNG"] = MediaType.IMAGE_PNG
            }
        }
    }

}

