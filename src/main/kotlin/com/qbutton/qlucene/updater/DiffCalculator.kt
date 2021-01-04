package com.qbutton.qlucene.updater

import com.qbutton.qlucene.dto.DiffCalculationResult
import com.qbutton.qlucene.dto.Operation
import difflib.Delta
import difflib.DiffUtils
import org.springframework.stereotype.Component
import java.util.stream.Collectors
import difflib.Delta.TYPE as DIFF_TYPE

@Component
class DiffCalculator {
    fun getDiff(oldTokens: List<String>, newTokens: List<String>): List<DiffCalculationResult> {
        val patch = DiffUtils.diff(oldTokens, newTokens)

        // we can do a proper map-reduce in the future, for now using the built-in functions for simplicity

        // map by same token
        val operationsByToken = patch.deltas
            .parallelStream()
            .map {
                when (it.type!!) {
                    DIFF_TYPE.INSERT -> toCreateResult(it.revised.lines)
                    Delta.TYPE.CHANGE -> toDeleteResult(it.original.lines) + toCreateResult(it.revised.lines)
                    Delta.TYPE.DELETE -> toDeleteResult(it.original.lines)
                }
            }
            .flatMap { it.stream() }
            .collect(Collectors.groupingBy { it.token })

        // reduce
        return operationsByToken.values
            .parallelStream()
            .map { it.reduce(this::reducePair) }
            .filter { it.count > 0 }
            .collect(Collectors.toList())
    }

    private fun reducePair(first: DiffCalculationResult, second: DiffCalculationResult): DiffCalculationResult {
        val token = first.token
        if (first.operation == second.operation) {
            return DiffCalculationResult(token, first.operation, first.count + second.count)
        }
        return if (first.count >= second.count)
            DiffCalculationResult(token, first.operation, first.count - second.count)
        else
            DiffCalculationResult(token, second.operation, second.count - first.count)
    }

    private fun toCreateResult(rawLines: List<String>) = toDiffCalculationResult(rawLines, Operation.CREATE)
    private fun toDeleteResult(rawLines: List<String>) = toDiffCalculationResult(rawLines, Operation.DELETE)

    private fun toDiffCalculationResult(rawLines: List<String>, operation: Operation) =
        rawLines.map { DiffCalculationResult(it, operation, 1) }
            .toMutableList()
}
