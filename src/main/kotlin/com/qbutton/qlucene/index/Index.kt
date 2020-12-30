package com.qbutton.qlucene.index

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.updater.Operation
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class Index : Executable {
    private val storage = ConcurrentHashMap<Term, ConcurrentHashMap<String, Int>>()

    fun find(term: Term): Set<DocumentSearchResult> {
        return storage
                .getOrDefault(term, Collections.emptyMap())
                .entries
                .map { DocumentSearchResult(it.key, it.value) }
                .filter { it.termOccurrences > 0 }
                .toSet()
    }

    fun update(term: Term, operation: Operation, fileId: String, count: Int) {
        val termMap = storage.computeIfAbsent(term) { ConcurrentHashMap() }
        val delta = if (operation == Operation.CREATE) count else -count;
        termMap.merge(fileId, delta, Integer::sum)
    }
}