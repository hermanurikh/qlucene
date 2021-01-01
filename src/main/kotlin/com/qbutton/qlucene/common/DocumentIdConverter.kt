package com.qbutton.qlucene.common

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/*
This class is to make some pseudo-random document ids for later usage as keys.
This is to unify them - as the paths used as keys may be very long of very short. Also it is an attempt to make their
 hashCode calculation not that dependent on input parameters.
 */
@Component
class DocumentIdConverter {
    private final val fileNameToId = ConcurrentHashMap<String, String>()

    fun toId(path: String): String {
        return fileNameToId.computeIfAbsent(path) { UUID.randomUUID().toString() }
    }

    fun toPath(id: String) = fileNameToId[id] ?: throw IllegalStateException("Mapping for id $id is absent")
}
