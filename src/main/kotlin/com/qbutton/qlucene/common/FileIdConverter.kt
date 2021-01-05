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
class FileIdConverter {
    private final val fileNameToId = ConcurrentHashMap<String, String>()
    private final val idToFileName = ConcurrentHashMap<String, String>()

    fun toId(path: String): String {
        val id = fileNameToId.computeIfAbsent(path) { UUID.randomUUID().toString() }
        idToFileName.putIfAbsent(id, path)
        return id
    }

    fun toPath(id: String) = idToFileName[id] ?: throw IllegalStateException("Mapping for id $id is absent")
}
