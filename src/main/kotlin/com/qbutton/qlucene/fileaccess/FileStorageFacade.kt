package com.qbutton.qlucene.fileaccess

import com.qbutton.qlucene.common.FileIdConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

/**
 * An API class for working with indexed files.
 */
@Component
class FileStorageFacade @Autowired constructor(
    private val fileIdConverter: FileIdConverter,
    private val inMemoryStorage: InMemoryStorage,
    private val fileSystemStorage: FileSystemStorage,
    private val fileSerializer: FileSerializer,
    @Value("\${indexed-contents.filesystem.threshold}")
    private val minLengthToSaveToFilesystem: Int
) {
    private val logger = LoggerFactory.getLogger(FileStorageFacade::class.java)

    fun getLastIndexedContents(fileId: String): String {
        // empty contents is a valid result, if we haven't indexed this file before (e.g. we are just adding it)
        val bytes = inMemoryStorage.readFile(fileId) ?: fileSystemStorage.readFile(fileId)
        return if (bytes != null) fileSerializer.toString(fileId, bytes) else ""
    }

    fun readRawTextFromFileSystem(fileId: String): String {
        val fileName = fileIdConverter.toPath(fileId)
        val path = Paths.get(fileName)
        // empty contents is a valid result since we might have removed the file
        return if (Files.exists(path)) path.toFile().readText() else ""
    }

    /**
     * Updates current indexed contents. This should be called with fileId lock held as it has CAS operations.
     */
    fun updateIndexedContents(fileId: String, fileContents: String) {
        val storage = if (shouldSaveInFileSystem(fileId, fileContents)) fileSystemStorage else inMemoryStorage
        logger.info("saving indexed contents for file $fileId")
        val bytes = fileSerializer.toBytes(fileId, fileContents)
        storage.addFile(fileId, bytes)
        logger.info("successfully updated indexed contents for file $fileId")
    }

    private fun shouldSaveInFileSystem(fileId: String, fileContents: String): Boolean {
        if (inMemoryStorage.hasFile(fileId)) {
            return false
        }
        if (fileSystemStorage.hasFile(fileId)) {
            return true
        }
        /*
            Some heuristics to decide where to save. In future, we could be smarter, e.g. check remaining memory,
            also relocate file from memory to filesystem when it grows.
         */
        return fileContents.length >= minLengthToSaveToFilesystem
    }
}
