package com.qbutton.qlucene.searcher

import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.index.Index
import com.qbutton.qlucene.searcher.mapper.Mapper
import com.qbutton.qlucene.searcher.reducer.Reducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SearchFacade @Autowired constructor(
    private val indices: List<Index>,
    private val reducers: List<Reducer>,
    private val mapper: Mapper
) {

    fun search(term: Term): List<String> {
        val documents = mutableListOf<DocumentSearchResult>()
        for (index in indices) {
            // we can have several indices for the same term type => we can scale by putting them to different hosts
            if (index.canExecute(term)) {
                documents.addAll(index.find(term))
            }
        }

        var reducedDocuments = documents
        for (reducer in reducers) {
            // we can have several reducers per term - to filter out on different conditions
            if (reducer.canExecute(term)) {
                reducedDocuments = reducer.reduce(reducedDocuments).toMutableList()
            }
        }

        return mapper.map(reducedDocuments)
    }
}
