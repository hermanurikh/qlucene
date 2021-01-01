package com.qbutton.qlucene.updater

import com.qbutton.qlucene.dto.DiffCalculationResult
import difflib.DiffUtils
import org.springframework.stereotype.Component

@Component
class DiffCalculator {
    fun getDiff(oldTokens: List<String>, newTokens: List<String>): List<DiffCalculationResult> {
        val patch = DiffUtils.diff(oldTokens, newTokens)
        // map reduce here
        TODO()
    }
}
