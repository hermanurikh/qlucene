package com.qbutton.qlucene.fileaccess

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * We are saving previously indexed contents either in memory or in file storage.
 */
abstract class Storage {

    abstract fun readFile(fileId: String): ByteArray?

    abstract fun addFile(fileId: String, fileContents: ByteArray)

    abstract fun hasFile(fileId: String): Boolean
}

@Component
class InMemoryStorage : Storage() {

    private val storage = ConcurrentHashMap<String, ByteArray>()

    override fun readFile(fileId: String) = storage[fileId]

    override fun addFile(fileId: String, fileContents: ByteArray) {
        storage[fileId] = fileContents
    }

    override fun hasFile(fileId: String) = storage.containsKey(fileId)
}

@Component
class FileSystemStorage @Autowired constructor(
    @Value("\${indexed-contents.root-dir}")
    private val rootDir: String
) : Storage() {
    override fun readFile(fileId: String): ByteArray? {
        return if (hasFile(fileId)) getPath(fileId).toFile().readBytes() else null
    }

    override fun addFile(fileId: String, fileContents: ByteArray) {
        Files.write(getPath(fileId), fileContents)
    }

    override fun hasFile(fileId: String): Boolean {
        val path = getPath(fileId)
        return Files.exists(path)
    }

    private fun getPath(fileId: String) = Paths.get(rootDir + fileId)
}
