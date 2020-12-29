package com.qbutton.qlucene.searcher.reducer

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.dto.DocumentSearchResult

interface Reducer : Executable {
    fun reduce(documents: List<DocumentSearchResult>) : List<DocumentSearchResult>
}