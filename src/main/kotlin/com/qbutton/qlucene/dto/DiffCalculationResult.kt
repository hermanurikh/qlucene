package com.qbutton.qlucene.dto

import com.qbutton.qlucene.updater.Operation

data class DiffCalculationResult(val token: String, val operation: Operation, val count: Int)
