package com.qbutton.qlucene.index

import com.qbutton.qlucene.common.Executable
import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.dto.DocumentSearchResult
import com.qbutton.qlucene.dto.Operation
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.dto.UpdateIndexInput
import java.util.concurrent.ConcurrentHashMap

/**
 * Base class for Index.
 *
 * Index is eventually consistent, it has no guarantees of changes done to files being immediately visible.
 *
 * Currently index is stored entirely in memory, we could expand it and save it on file
 * system as well (as it is done in FileFacade.kt for previously indexed contents). Or use some 3rd party storage
 * which provides in-memory or filesystem dispatching out-of-the-box.
 */
abstract class Index : Executable, Resettable {
    // reverse index -> mapping of Term to a map of [file id -> number of occurrences in file]
    private val reverseIndex = ConcurrentHashMap<Term, ConcurrentHashMap<String, Int>>()
    // main index -> mapping of file id to a list of terms it contains
    private val forwardIndex = ConcurrentHashMap<String, Map<Term, Int>>()

    /**
     * Searches for given term.
     */
    fun find(term: Term): Set<DocumentSearchResult> {
        return reverseIndex[term]
            ?.entries
            // map and then filter, not vice versa - or we may get entries updated by other thread after filtering
            ?.map { DocumentSearchResult(it.key, it.value) }
            ?.filter { it.termOccurrences > 0 }
            ?.toSet() ?: emptySet()
    }

    fun findByDocId(fileId: String) = forwardIndex[fileId] ?: emptyMap()

    fun updateReverseIndex(fileId: String, terms: Map<Term, Int>) {
        forwardIndex[fileId] = terms
    }

    fun update(updateInfo: UpdateIndexInput) {
        val termMap = reverseIndex.computeIfAbsent(updateInfo.term) { ConcurrentHashMap() }
        val delta = if (updateInfo.operation == Operation.CREATE) updateInfo.count else -updateInfo.count
        termMap.merge(updateInfo.fileId, delta, Integer::sum)
    }

    override fun resetState() {
        reverseIndex.clear()
        forwardIndex.clear()
    }
}
