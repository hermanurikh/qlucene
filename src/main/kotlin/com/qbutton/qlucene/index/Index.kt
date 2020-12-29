package com.qbutton.qlucene.index

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.updater.Operation

interface Index : Executable {
    fun find(term: Term): Set<DocumentSearchResult>

    fun update(term: Term, operation: Operation, fileId: String)
}