package com.qbutton.qlucene.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * This class holds information about roots which are registered and monitored in the system.
 */
@Component
class RegisteredRoots @Autowired constructor(
    private val fileIdConverter: FileIdConverter,
) : Resettable {

    private val storage = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    /**
     * Checks if exactly this path is a monitored root.
     */
    fun isRegisteredAsRoot(path: Path): Boolean {
        val absolutePath = path.toAbsolutePath()
        val fileId = fileIdConverter.toId(absolutePath)
        return storage.contains(fileId)
    }

    /**
     * Checks if this path is monitored as part of some registered root.
     */
    fun isMonitored(path: String): Boolean {
        var absolutePath = Paths.get(path).toAbsolutePath()
        while (absolutePath != null) {
            if (isRegisteredAsRoot(absolutePath)) {
                return true
            }
            absolutePath = absolutePath.parent
        }

        return false
    }

    fun add(path: Path) {
        val pathId = fileIdConverter.toId(path.toAbsolutePath())
        storage.add(pathId)
    }

    override fun resetState() {
        storage.clear()
    }
}
