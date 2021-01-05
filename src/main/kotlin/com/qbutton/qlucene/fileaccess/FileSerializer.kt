package com.qbutton.qlucene.fileaccess

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * A class to serialize contents.
 * Contents are compressed using GZip, if they are greater than some threshold.
 * During my tests, compressed contents of english dictionary save 3x space. Compressed code
 * saves much more as tokens repeat more often.
 */
@Component
class FileSerializer @Autowired constructor(
    @Value("\${indexed-contents.compression.threshold}")
    private val compressionThreshold: Int
) {
    private val charset = StandardCharsets.UTF_8
    private val compressionMapping = ConcurrentHashMap<String, Boolean>()
    private val logger = LoggerFactory.getLogger(FileSerializer::class.java)

    fun toBytes(fileId: String, contents: String): ByteArray {
        var needsCompression = needsCompression(contents)
        val prevMapping = compressionMapping.putIfAbsent(fileId, needsCompression)
        if (prevMapping != null) {
            needsCompression = prevMapping
        }

        logger.info("serializing file $fileId, compressed: $needsCompression")

        return if (needsCompression) compress(contents) else contents.toByteArray(charset)
    }

    fun toString(fileId: String, contents: ByteArray): String {
        val needsDecompression = compressionMapping[fileId]
            ?: throw IllegalStateException("No info about compression found for $fileId")

        logger.info("de-serializing file $fileId, to be decompressed: $needsDecompression")

        return if (needsDecompression) decompress(contents) else String(contents, charset)
    }

    private fun needsCompression(contents: String) = contents.length >= compressionThreshold

    private fun compress(contents: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos)
            .use { gzipOutputStream ->
                gzipOutputStream.write(contents.toByteArray(charset))
            }
        return bos.toByteArray()
    }

    private fun decompress(contents: ByteArray): String {
        val bis = ByteArrayInputStream(contents)
        GZIPInputStream(bis)
            .use { gzipInputStream ->
                val bytes = gzipInputStream.readAllBytes()
                return String(bytes, charset)
            }
    }
}
