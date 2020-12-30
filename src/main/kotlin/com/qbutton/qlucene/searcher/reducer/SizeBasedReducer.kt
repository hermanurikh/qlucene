package com.qbutton.qlucene.searcher.reducer

import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.searcher.ranker.SizeBasedRanker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.PriorityQueue

@Component
class SizeBasedReducer @Autowired constructor(
    private val ranker: SizeBasedRanker,
    @Value("\${reducer.size-based.max-size}")
    private val maxSize: Int
) : Reducer  {
    override fun reduce(documents: List<DocumentSearchResult>): List<DocumentSearchResult> {

        if (documents.size <= maxSize) {
            return documents.sortedWith(ranker)
        }

        //a heap to find top K elements, reversed comparison for "losing" elements to be on top of the heap
        val heap = PriorityQueue<DocumentSearchResult> { o1, o2 -> ranker.compare(o2, o1) }

        for (document in documents) {
            heap.offer(document)
            if (heap.size > maxSize ) {
                heap.poll()
            }
        }

        return heap.toMutableList().sortedWith(ranker)
    }

    override fun canExecute(term: Term) = true
}