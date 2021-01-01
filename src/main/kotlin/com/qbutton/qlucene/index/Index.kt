package com.qbutton.qlucene.index

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Operation
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.dto.UpdateIndexInput
import java.util.concurrent.ConcurrentHashMap

abstract class Index : Executable {
    private val storage = ConcurrentHashMap<Term, ConcurrentHashMap<String, Int>>()

    // TODO think about ignoring case
    fun find(term: Term): Set<DocumentSearchResult> {
        return storage[term]
            ?.entries
            ?.map { DocumentSearchResult(it.key, it.value) }
            ?.filter { it.termOccurrences > 0 }
            ?.toSet() ?: emptySet()
    }

    fun update(updateInfo: UpdateIndexInput) {
        val termMap = storage.computeIfAbsent(updateInfo.term) { ConcurrentHashMap() }
        val delta = if (updateInfo.operation == Operation.CREATE) updateInfo.count else -updateInfo.count
        termMap.merge(updateInfo.fileId, delta, Integer::sum)
    }
}
