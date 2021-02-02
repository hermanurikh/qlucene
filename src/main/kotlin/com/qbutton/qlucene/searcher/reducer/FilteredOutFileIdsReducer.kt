package com.qbutton.qlucene.searcher.reducer

import com.qbutton.qlucene.common.FileIdsToRemove
import com.qbutton.qlucene.dto.DocumentSearchResult
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class FilteredOutFileIdsReducer(
    private val fileIdsToRemove: FileIdsToRemove
) : Reducer() {
    override fun reduce(documents: List<DocumentSearchResult>) = documents.filter {
        !fileIdsToRemove.contains(it.fileId)
    }
}
