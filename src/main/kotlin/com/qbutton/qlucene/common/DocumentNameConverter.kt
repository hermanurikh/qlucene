package com.qbutton.qlucene.common

import org.springframework.stereotype.Component
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/*
This class is to make some pseudo-random document ids for later usage as keys.
This is to unify them - as the paths used as keys may be very long of very short. Also it is an attempt to make their
 hashCode calculation not that dependent on input parameters.
 */
@Component
class DocumentNameConverter {
    private final val fileNameToId = ConcurrentHashMap<String,String>()

    fun toId(name: String) : String {
        return fileNameToId.computeIfAbsent(name) { UUID.randomUUID().toString() }
    }

    fun toName(id: String) = fileNameToId[id] ?: throw IllegalStateException("Mapping for id $id is absent")
}