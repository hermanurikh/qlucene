package com.qbutton.qlucene.searcher

import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.index.Index
import com.qbutton.qlucene.searcher.mapper.Mapper
import com.qbutton.qlucene.searcher.reducer.Reducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.IllegalStateException

@Component
class SearchFacade @Autowired constructor(
    private val indexes: List<Index>,
    private val reducers: List<Reducer>,
    private val mappers: List<Mapper>
) {

    fun search(term: Term): List<String> {
        val documents = mutableListOf<DocumentSearchResult>()
        for (index in indexes) {
            // we can have several indexes for the same term type => we can scale by putting them to different hosts
            if (index.canExecute(term)) {
                documents.addAll(index.find(term))
            }
        }

        val reducer = reducers.find { it.canExecute(term) }
        val reducedDocuments = reducer?.reduce(documents) ?: documents

        val mapper = mappers.find { it.canExecute(term) }

        return mapper?.map(reducedDocuments)
            ?: throw IllegalStateException("No mapper found to map documents to strings")
    }
}
