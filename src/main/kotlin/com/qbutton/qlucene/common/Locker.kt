package com.qbutton.qlucene.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * A locker class to be able to globally and consistently lock on the same entity for same fileId / path.
 * Not using built-in synchronized ones for additional flexibility.
 */
@Component
class Locker @Autowired constructor(
    private val fileIdConverter: FileIdConverter
) : Resettable {

    private val fileLocks = ConcurrentHashMap<String, Lock>()

    fun lockId(fileId: String) {
        val lock = fileLocks.computeIfAbsent(fileId) { ReentrantLock() }
        lock.lock()
    }

    fun lockPath(path: String) {
        val pathId = fileIdConverter.toId(path)
        lockId(pathId)
    }

    fun unlockId(fileId: String) {
        val lock = fileLocks[fileId] ?: throw IllegalStateException("Cannot unlock, lock for $fileId is absent")
        lock.unlock()
    }

    fun unlockPath(path: String) {
        val pathId = fileIdConverter.toId(path)
        unlockId(pathId)
    }

    override fun resetState() {
        fileLocks.clear()
    }
}
