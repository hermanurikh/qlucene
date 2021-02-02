package com.qbutton.qlucene.dto

/**
 * Result of user trying to register something. We could also add status here or any more details if needed.
 */
abstract class RegistrationResult(private val message: String) {
    fun getMessage() = message
}

class FileRegistrationSuccessful(path: String) : RegistrationResult("Successfully registered file: $path")
class FileAlreadyRegistered(path: String) : RegistrationResult("File $path is already registered")
class FileSizeExceedsLimits(path: String) : RegistrationResult("File $path size exceeds configured limits, it will not be indexed")
class FileFormatUnsupported(path: String) : RegistrationResult("File $path format is not supported")

class DirectoryRegistrationSuccessful(path: String) : RegistrationResult("Successfully registered directory: $path")
class DirectoryRegistrationCancelled(path: String) : RegistrationResult("Directory $path indexing was cancelled")
class DirectoryAlreadyRegistered(path: String) : RegistrationResult("Directory $path is already registered")

class AbnormalFileRegistrationResult(path: String) : RegistrationResult("File $path is not a directory or a normal file")
class FileNotFoundRegistrationResult(path: String) : RegistrationResult("File $path was not found")
