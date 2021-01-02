package com.qbutton.qlucene.fileaccess

import com.qbutton.qlucene.common.FileIdConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Paths

/**
 * An API class for working with indexed files.
 */
@Component
class FileFacade @Autowired constructor(
    private val fileIdConverter: FileIdConverter,
    private val inMemoryStorage: InMemoryStorage,
    private val fileSystemStorage: FileSystemStorage
) {
    fun getLastIndexedContents(fileId: String): String {
        // empty contents is a valid result, if we haven't indexed this file before (e.g. we are just adding it)
        return inMemoryStorage.readFile(fileId) ?: fileSystemStorage.readFile(fileId) ?: ""
    }

    fun readFromFileSystem(fileId: String): String {
        val fileName = fileIdConverter.toPath(fileId)
        return Paths.get(fileName).toFile().readText()
    }

    fun updateIndexedContents(fileId: String, fileContents: String) {
        TODO()
    }
}
