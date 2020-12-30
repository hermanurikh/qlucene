package com.qbutton.qlucene.common

import com.qbutton.qlucene.dto.Term

interface Executable {
    fun canExecute(term: Term): Boolean
}
