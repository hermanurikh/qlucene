package com.qbutton.qlucene.fileaccess

import com.qbutton.qlucene.common.DocumentIdConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FileReader @Autowired constructor(
    private val documentIdConverter: DocumentIdConverter
) {
    fun getLastIndexedContents(fileId: String): String {
        // we can even not found anything here if operation is add
        TODO()
    }

    fun readFromFileSystem(fileId: String): String {
        val fileName = documentIdConverter.toPath(fileId)
        TODO()
    }
}
