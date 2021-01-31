package com.qbutton.qlucene.searcher.reducer

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.FilteredOutRoots
import com.qbutton.qlucene.dto.DocumentSearchResult
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class FilteredOutRootsReducer(
    private val fileIdConverter: FileIdConverter,
    private val filteredOutRoots: FilteredOutRoots
) : Reducer() {
    override fun reduce(documents: List<DocumentSearchResult>) = documents.filter {
        val stringPath = fileIdConverter.toPath(it.fileId)
        !filteredOutRoots.shouldFilterOut(stringPath)
    }
}
