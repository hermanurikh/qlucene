package com.qbutton.qlucene.dto

import com.qbutton.qlucene.updater.Operation

data class UpdateIndexInput(val term: Term, val operation: Operation, val fileId: String, val count: Int)
