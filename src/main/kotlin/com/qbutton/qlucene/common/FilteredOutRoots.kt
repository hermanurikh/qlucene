package com.qbutton.qlucene.common

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * This class holds information about roots which should be filtered out during search or events catching because
 * this root has been removed from index.
 */
@Component
class FilteredOutRoots(
    private val registeredRoots: RegisteredRoots,
) : Resettable {
    private val logger = LoggerFactory.getLogger(FilteredOutRoots::class.java)
    private val storage = Collections.newSetFromMap(ConcurrentHashMap<Path, Boolean>())

    /**
     * Checks if the path should be filtered out.
     * We traverse up the path and see whatever we meet first. If we meet a filtered out path, we do filter,
     * else if we meet a registered root, we don't.
     */
    fun shouldFilterOut(path: String): Boolean {
        var absolutePath = Paths.get(path).toAbsolutePath()
        while (absolutePath != null) {
            if (storage.contains(absolutePath)) {
                return true
            } else if (registeredRoots.isRegisteredAsRoot(absolutePath)) {
                return false
            }
            absolutePath = absolutePath.parent
        }
        return false
    }

    fun add(filteredRoot: Path) {
        val absolutePath = filteredRoot.toAbsolutePath()
        logger.info("adding $absolutePath to filtered out roots")
        storage.add(absolutePath)
    }

    fun remove(filteredRoot: Path) {
        val absolutePath = filteredRoot.toAbsolutePath()
        logger.info("removing $absolutePath from filtered out roots")
        storage.remove(absolutePath)
    }

    fun remove(filteredRoot: String) = remove(Paths.get(filteredRoot))

    override fun resetState() {
        storage.clear()
    }
}
