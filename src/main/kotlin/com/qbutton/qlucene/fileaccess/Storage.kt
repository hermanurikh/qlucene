package com.qbutton.qlucene.fileaccess

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * We are saving previously indexed contents either in memory or in file storage (in case we are running out of memory).
 * Contents are compressed using GZip, if they are greater than some threshold.
 *
 * During my tests, compressed contents of english dictionary save 3x space. Compressed code
 * saves much more as tokens repeat more often.
 */
abstract class Storage constructor(
    private val fileCompressor: FileCompressor
) {
    fun readFile(fileId: String): String? {
        val compressedContents = readCompressedFile(fileId)
        return if (compressedContents != null) fileCompressor.decompress(compressedContents) else null
    }

    fun addFile(fileId: String, fileContents: String) {
        val compressedContents = fileCompressor.compress(fileContents)
        addCompressedFile(fileId, compressedContents)
    }

    abstract fun readCompressedFile(fileId: String): ByteArray?

    abstract fun addCompressedFile(fileId: String, compressedFileContents: ByteArray)
}

@Component
class InMemoryStorage @Autowired constructor(
    fileCompressor: FileCompressor
) : Storage(fileCompressor) {

    private val storage = ConcurrentHashMap<String, ByteArray>()

    override fun readCompressedFile(fileId: String) = storage[fileId]

    override fun addCompressedFile(fileId: String, compressedFileContents: ByteArray) {
        storage[fileId] = compressedFileContents
    }
}

@Component
class FileSystemStorage @Autowired constructor(
    fileCompressor: FileCompressor,
    @Value("\$indexed-contents.root-dir")
    private val rootDir: String
) : Storage(fileCompressor) {
    override fun readCompressedFile(fileId: String): ByteArray? {
        val path = getPath(fileId)
        return if (Files.exists(path)) path.toFile().readBytes() else null
    }

    override fun addCompressedFile(fileId: String, compressedFileContents: ByteArray) {
        Files.write(getPath(fileId), compressedFileContents)
    }

    private fun getPath(fileId: String) = Paths.get(rootDir + fileId)
}
