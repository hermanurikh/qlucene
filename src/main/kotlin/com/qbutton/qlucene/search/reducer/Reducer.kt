package com.qbutton.qlucene.search.reducer

import com.qbutton.qlucene.common.Acceptable
import com.qbutton.qlucene.dto.DocumentSearchResult

interface Reducer : Acceptable {
    fun reduce(documents: List<DocumentSearchResult>) : List<DocumentSearchResult>
}