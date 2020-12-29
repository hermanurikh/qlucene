package com.qbutton.qlucene.searcher.mapper

import com.qbutton.qlucene.common.DocumentNameConverter
import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FileNameMapper @Autowired constructor(
        private val documentNameConverter: DocumentNameConverter
) : Mapper {
    override fun map(documents: List<DocumentSearchResult>) = documents.map { documentNameConverter.toName(it.fileId) }

    override fun canExecute(term: Term) = true
}