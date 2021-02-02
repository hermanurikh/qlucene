package com.qbutton.qlucene.updater

import com.qbutton.qlucene.common.FileIdConverter
import com.qbutton.qlucene.common.FileIdsToRemove
import com.qbutton.qlucene.common.FileValidator
import com.qbutton.qlucene.common.FilteredOutRoots
import com.qbutton.qlucene.common.IndexCanceller
import com.qbutton.qlucene.common.Locker
import com.qbutton.qlucene.common.RegisteredRoots
import com.qbutton.qlucene.dto.DirectoryAlreadyRegistered
import com.qbutton.qlucene.dto.DirectoryRegistrationCancelled
import com.qbutton.qlucene.dto.DirectoryRegistrationSuccessful
import com.qbutton.qlucene.dto.DirectoryUnregistrationSuccessful
import com.qbutton.qlucene.dto.FileAlreadyRegistered
import com.qbutton.qlucene.dto.FileNotFoundRegistrationResult
import com.qbutton.qlucene.dto.FileRegistrationSuccessful
import com.qbutton.qlucene.dto.FileUnregistrationSuccessful
import com.qbutton.qlucene.dto.NotRegistered
import com.qbutton.qlucene.dto.RegistrationResult
import com.qbutton.qlucene.dto.UnregistrationResult
import com.qbutton.qlucene.updater.background.WatchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Component
class FileRegistrationFacade @Autowired constructor(
    private val locker: Locker,
    private val watchService: WatchService,
    private val fileValidator: FileValidator,
    private val indexCanceller: IndexCanceller,
    private val registeredRoots: RegisteredRoots,
    private val fileIdConverter: FileIdConverter,
    private val fileIdsToRemove: FileIdsToRemove,
    private val filteredOutRoots: FilteredOutRoots,
    private val fileUpdaterFacade: FileUpdaterFacade
) {

    fun register(path: String): RegistrationResult {
        val filePath = Paths.get(path).toAbsolutePath()
        val stringAbsolutePath = filePath.toString()
        val file = filePath.toFile()
        val fileId = fileIdConverter.toId(filePath)

        val validationResult = validatePath(filePath)
        if (validationResult != null) return validationResult

        try {
            locker.lockId(fileId)
            val isFile = file.isFile

            if (registeredRoots.isMonitored(stringAbsolutePath)) {
                // we are already watching this path
                return if (filteredOutRoots.shouldFilterOut(stringAbsolutePath)) {
                    // and we filter it out => stop doing it for this exact path and add as new root
                    filteredOutRoots.remove(stringAbsolutePath)
                    registeredRoots.add(filePath)
                    if (isFile) FileRegistrationSuccessful(path) else DirectoryRegistrationSuccessful(path)
                } else {
                    // and we don't filter it out => already registered
                    if (isFile) FileAlreadyRegistered(path) else DirectoryAlreadyRegistered(path)
                }
            } else {
                return if (isFile) {
                    watchService.attachWatcherToFile(filePath)
                    fileUpdaterFacade.update(fileId)
                    registeredRoots.add(filePath)
                    FileRegistrationSuccessful(path)
                } else {
                    val (watcher, addedFileIds) = watchService.attachWatcherToRootDirAndIndex(filePath)
                    if (!indexCanceller.isCancelled(fileId)) {
                        registeredRoots.add(filePath)
                        // these fileIds may be there from previous indexing waiting for clean-up, so remove them
                        fileIdsToRemove.removeAll(addedFileIds)
                        DirectoryRegistrationSuccessful(path)
                    } else {
                        watcher.close()
                        // add addedFileIds to blacklist and clean up
                        fileIdsToRemove.addAll(addedFileIds)
                        indexCanceller.resetState(fileId)
                        DirectoryRegistrationCancelled(path)
                    }
                }
            }
        } finally {
            locker.unlockId(fileId)
        }
    }

    fun unregister(path: String): UnregistrationResult {
        val filePath = Paths.get(path).toAbsolutePath()
        if (!registeredRoots.isMonitored(path) || filteredOutRoots.shouldFilterOut(path)) {
            return NotRegistered(path)
        }
        filteredOutRoots.add(filePath)
        return if (Files.isDirectory(filePath))
            DirectoryUnregistrationSuccessful(path)
        else FileUnregistrationSuccessful(path)
    }

    private fun validatePath(path: Path): RegistrationResult? {
        val file = path.toFile()
        val stringPath = path.toString()

        if (!file.exists()) {
            return FileNotFoundRegistrationResult(stringPath)
        }

        return if (file.isFile) fileValidator.validateFileOkForRegistration(path) else null
    }
}
