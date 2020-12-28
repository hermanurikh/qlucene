package com.qbutton.qlucene.search

import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.index.Index
import com.qbutton.qlucene.search.mapper.Mapper
import com.qbutton.qlucene.search.reducer.Reducer
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
            if (index.accepts(term)) {
                documents.addAll(index.find(term))
            }
        }

        val reducer = reducers.find { it.accepts(term) }
        val reducedDocuments = reducer?.reduce(documents) ?: documents

        val mapper = mappers.find { it.accepts(term) }

        return mapper?.map(reducedDocuments)
                ?: throw IllegalStateException("No mapper found to map documents to strings")
    }
}