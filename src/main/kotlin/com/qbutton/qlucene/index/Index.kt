package com.qbutton.qlucene.index

import com.qbutton.qlucene.common.Acceptable
import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term

interface Index : Acceptable {
    fun find(term: Term): Set<DocumentSearchResult>
}