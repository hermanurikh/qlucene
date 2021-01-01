package com.qbutton.qlucene.searcher.mapper

import com.qbutton.qlucene.dto.DocumentSearchResult

abstract class Mapper {
    abstract fun map(documents: List<DocumentSearchResult>): List<String>
}
