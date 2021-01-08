package com.qbutton.qlucene.common

/**
 * An interface to oblige descendants to support state resetting.
 */
interface Resettable {
    /**
     * This method is not supposed to be thread safe. It is done for instrumental purposes only.
     */
    fun resetState()
}
