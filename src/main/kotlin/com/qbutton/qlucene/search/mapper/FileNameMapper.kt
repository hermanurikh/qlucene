package com.qbutton.qlucene.search.mapper

import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import org.springframework.stereotype.Component

@Component
class FileNameMapper : Mapper {
    override fun map(documents: List<DocumentSearchResult>) = documents.map { it.fileName }

    override fun accepts(term: Term) = true
}