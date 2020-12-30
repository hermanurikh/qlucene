package com.qbutton.qlucene.searcher.reducer

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.dto.DocumentSearchResult

abstract class Reducer : Executable {
    abstract fun reduce(documents: List<DocumentSearchResult>): List<DocumentSearchResult>
}
