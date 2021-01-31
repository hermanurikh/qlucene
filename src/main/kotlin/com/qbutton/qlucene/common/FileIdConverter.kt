package com.qbutton.qlucene.common

import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/*
This class is to make some pseudo-random document ids for later usage as keys.
This is to unify them - as the paths used as keys may be very long of very short. Also it is an attempt to make their
 hashCode calculation not that dependent on input parameters.
 */
@Component
class FileIdConverter : Resettable {
    private final val fileNameToId = ConcurrentHashMap<String, String>()
    private final val idToFileName = ConcurrentHashMap<String, String>()

    fun toId(path: Path): String {
        val stringPath = path.toString()
        val id = fileNameToId.computeIfAbsent(stringPath) { UUID.randomUUID().toString() }
        idToFileName.putIfAbsent(id, stringPath)
        return id
    }

    fun toPath(id: String) = idToFileName[id] ?: throw IllegalStateException("Mapping for id $id is absent")

    override fun resetState() {
        fileNameToId.clear()
        idToFileName.clear()
    }
}
