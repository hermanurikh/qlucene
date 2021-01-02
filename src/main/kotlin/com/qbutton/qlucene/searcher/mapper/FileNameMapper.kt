package com.qbutton.qlucene.searcher.mapper

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.dto.DocumentSearchResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FileNameMapper @Autowired constructor(
    private val fileIdConverter: FileIdConverter
) : Mapper() {
    override fun map(documents: List<DocumentSearchResult>) = documents.map { fileIdConverter.toPath(it.fileId) }
}
