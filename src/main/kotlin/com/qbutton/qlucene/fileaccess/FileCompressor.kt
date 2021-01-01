package com.qbutton.qlucene.fileaccess

import org.springframework.stereotype.Component

@Component
class FileCompressor {

    fun compress(contents: String): ByteArray {
        TODO("also check file size and noop if small")
    }

    fun decompress(compressedContents: ByteArray): String {
        TODO()
    }
}
