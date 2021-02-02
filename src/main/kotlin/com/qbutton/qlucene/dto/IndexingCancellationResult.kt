package com.qbutton.qlucene.dto

/**
 * Result of user trying to register something. We could also add status here or any more details if needed.
 */
abstract class IndexingCancellationResult(private val message: String) {
    fun getMessage() = message
}

class IndexingCancellationSuccessful(path: String) : IndexingCancellationResult("Successfully requested cancellation for path: $path")
class IndexingAlreadyCancelled(path: String) : IndexingCancellationResult("Path $path has been already requested index cancellation")
