package com.qbutton.qlucene.updater

import com.qbutton.qlucene.dto.DiffCalculationResult
import com.qbutton.qlucene.dto.Operation
import com.qbutton.qlucene.dto.Term
import org.springframework.stereotype.Component

/**
 * A class responsible for getting diffs between old tokens list and new tokens list.
 *
 * Originally I used a 3rd party com.googlecode.java-diff-utils:diffutils for it. Turned out, it has O(n^2) complexity,
 * because it calculates the positions in which things are changed, and it blew up on big files. I don't need the positions,
 * so I can use a simpler algo to find the diff in O(n).
 */
@Component
class DiffCalculator {
    fun getDiff(oldTokens: List<Term>, newTokens: List<Term>): List<DiffCalculationResult> {

        val diffCalculationResults = mutableListOf<DiffCalculationResult>()

        val oldTokensGrouped = oldTokens.groupingBy { it }.eachCount().toMap()
        val newTokensGrouped = newTokens.groupingBy { it }.eachCount().toMutableMap()

        oldTokensGrouped.forEach { (token, count) ->
            val updatedTokenCount = newTokensGrouped[token] ?: 0
            when {
                count > updatedTokenCount -> {
                    // some deletions happened
                    diffCalculationResults.add(DiffCalculationResult(token, Operation.DELETE, count - updatedTokenCount))
                }
                count < updatedTokenCount -> {
                    // some additions happened
                    diffCalculationResults.add(DiffCalculationResult(token, Operation.CREATE, updatedTokenCount - count))
                }
            }

            newTokensGrouped.remove(token)
        }

        // check tokens which are still in newTokens map -> these were added
        diffCalculationResults.addAll(
            newTokensGrouped.map { DiffCalculationResult(it.key, Operation.CREATE, it.value) }
        )

        return diffCalculationResults
    }
}
