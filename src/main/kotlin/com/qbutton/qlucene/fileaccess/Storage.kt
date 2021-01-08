package com.qbutton.qlucene.fileaccess

import com.qbutton.qlucene.common.Resettable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * We are saving previously indexed contents either in memory or in file storage.
 */
abstract class Storage : Resettable {

    abstract fun readFile(fileId: String): ByteArray?

    abstract fun addFile(fileId: String, fileContents: ByteArray)

    abstract fun hasFile(fileId: String): Boolean
}

@Component
class InMemoryStorage : Storage() {

    private val storage = ConcurrentHashMap<String, ByteArray>()
    private val logger = LoggerFactory.getLogger(InMemoryStorage::class.java)

    override fun readFile(fileId: String): ByteArray? {
        logger.info("reading file $fileId from in-memory storage")
        return storage[fileId]
    }

    override fun addFile(fileId: String, fileContents: ByteArray) {
        logger.info("adding file $fileId to in-memory storage")
        storage[fileId] = fileContents
    }

    override fun hasFile(fileId: String) = storage.containsKey(fileId)

    override fun resetState() {
        storage.clear()
    }
}

@Component
class FileSystemStorage @Autowired constructor(
    @Value("\${indexed-contents.root-dir}")
    private val rootDir: String
) : Storage() {
    private val logger = LoggerFactory.getLogger(FileSystemStorage::class.java)

    @PostConstruct
    fun initTmpDir() {
        val path = Paths.get(rootDir)
        if (!Files.exists(path)) {
            Files.createDirectories(path)
            logger.info("directory $path created")
        }
    }

    @PreDestroy
    fun cleanTmpDir() {
        val rootPath = Paths.get(rootDir)
        val contents = rootPath.toFile().listFiles()
        if (contents != null) {
            for (file in contents) {
                Files.delete(file.toPath())
            }
        }
        Files.delete(rootPath)
        logger.info("directory $rootPath removed")
    }

    override fun readFile(fileId: String): ByteArray? {
        logger.info("reading file $fileId from file system")
        return if (hasFile(fileId)) getPath(fileId).toFile().readBytes() else null
    }

    override fun addFile(fileId: String, fileContents: ByteArray) {
        logger.info("adding file $fileId to file system storage")
        Files.write(getPath(fileId), fileContents)
    }

    override fun hasFile(fileId: String): Boolean {
        val path = getPath(fileId)
        return Files.exists(path)
    }

    override fun resetState() {
        cleanTmpDir()
        initTmpDir()
    }

    private fun getPath(fileId: String) = Paths.get(rootDir + fileId)
}
