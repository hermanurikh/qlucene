package com.qbutton.qlucene.search.mapper

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.dto.DocumentSearchResult

interface Mapper : Executable {
    fun map(documents: List<DocumentSearchResult>) : List<String>
}