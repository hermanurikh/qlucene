package com.qbutton.qlucene.dto

/**
 * Result of user trying to register something. We could also add status here or any more details if needed.
 */
abstract class UnregistrationResult(private val message: String) {
    fun getMessage() = message
}

class FileUnregistrationSuccessful(path: String) : UnregistrationResult("Successfully unregistered file: $path")
class DirectoryUnregistrationSuccessful(path: String) : UnregistrationResult("Successfully unregistered directory: $path")

class NotRegistered(path: String) : UnregistrationResult("File/directory $path is not registered")
