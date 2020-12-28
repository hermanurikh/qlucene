package com.qbutton.qlucene.search.mapper

import com.qbutton.qlucene.common.Acceptable
import com.qbutton.qlucene.dto.DocumentSearchResult

interface Mapper : Acceptable {
    fun map(documents: List<DocumentSearchResult>) : List<String>
}