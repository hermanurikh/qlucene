package com.qbutton.qlucene.search.ranker

import com.qbutton.qlucene.dto.DocumentSearchResult
import org.springframework.stereotype.Component

@Component
class SizeBasedRanker : Comparator<DocumentSearchResult> {
    override fun compare(o1: DocumentSearchResult?, o2: DocumentSearchResult?)
            = o2!!.termOccurrences.compareTo(o1!!.termOccurrences)
}