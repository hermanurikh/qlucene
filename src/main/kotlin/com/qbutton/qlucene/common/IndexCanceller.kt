package com.qbutton.qlucene.common

import com.qbutton.qlucene.dto.IndexingAlreadyCancelled
import com.qbutton.qlucene.dto.IndexingCancellationResult
import com.qbutton.qlucene.dto.IndexingCancellationSuccessful
import org.springframework.stereotype.Component
import java.nio.file.Paths
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@Component
class IndexCanceller(
    private val fileIdConverter: FileIdConverter,
) : Resettable {

    private val cancelledPaths = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun cancel(path: String): IndexingCancellationResult {
        val absolutePath = Paths.get(path).toAbsolutePath()
        val id = fileIdConverter.toId(absolutePath)
        return if (cancelledPaths.add(id))
            IndexingCancellationSuccessful(path)
        else IndexingAlreadyCancelled(path)
    }

    fun isCancelled(fileId: String) = cancelledPaths.contains(fileId)

    fun resetState(fileId: String) = cancelledPaths.remove(fileId)

    override fun resetState() {
        cancelledPaths.clear()
    }
}
