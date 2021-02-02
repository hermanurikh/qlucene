package com.qbutton.qlucene.common

import com.qbutton.qlucene.dto.AbnormalFileRegistrationResult
import com.qbutton.qlucene.dto.FileFormatUnsupported
import com.qbutton.qlucene.dto.FileSizeExceedsLimits
import com.qbutton.qlucene.dto.RegistrationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.nio.file.Files
import java.nio.file.Path

@Component
class FileValidator(
    @Value("\${file.max-indexed-size}")
    private val maxFileSize: Long,
    @Value("\${file.supported-extensions}")
    private val supportedExtensions: Set<String>
) {

    fun validateFileOkForRegistration(path: Path): RegistrationResult? {
        val stringPath = path.toString()

        if (!Files.isRegularFile(path)) {
            return AbnormalFileRegistrationResult(stringPath)
        }

        if (Files.size(path) > maxFileSize) {
            return FileSizeExceedsLimits(stringPath)
        }
        if (!supportedExtensions.contains(StringUtils.getFilenameExtension(stringPath))) {
            return FileFormatUnsupported(stringPath)
        }

        return null
    }

    fun isValid(path: Path) = validateFileOkForRegistration(path) == null
}
