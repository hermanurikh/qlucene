package com.qbutton.qlucene.searcher.mapper

import com.qbutton.qlucene.common.DocumentIdConverter
import com.qbutton.qlucene.dto.DocumentSearchResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FileNameMapper @Autowired constructor(
    private val documentIdConverter: DocumentIdConverter
) : Mapper() {
    override fun map(documents: List<DocumentSearchResult>) = documents.map { documentIdConverter.toPath(it.fileId) }
}
