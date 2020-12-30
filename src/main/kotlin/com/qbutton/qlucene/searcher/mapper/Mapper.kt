package com.qbutton.qlucene.searcher.mapper

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.dto.DocumentSearchResult

abstract class Mapper : Executable {
    abstract fun map(documents: List<DocumentSearchResult>): List<String>
}
