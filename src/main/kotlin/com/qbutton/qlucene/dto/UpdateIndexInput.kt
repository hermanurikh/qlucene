package com.qbutton.qlucene.dto

data class UpdateIndexInput(val term: Term, val operation: Operation, val fileId: String, val count: Int)
