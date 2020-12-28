package com.qbutton.qlucene.common

import com.qbutton.qlucene.dto.Term

interface Acceptable {
    fun accepts(term: Term) : Boolean
}