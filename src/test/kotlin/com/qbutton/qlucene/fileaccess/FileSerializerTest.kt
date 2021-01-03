package com.qbutton.qlucene.fileaccess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FileSerializerTest {

    private val fileSerializer = FileSerializer(compressionThreshold = 20)

    @Test
    fun `serialize and deserialize should be consistent when not compressed`() {
        val weirdString = "lazy 𓃡"
        val fileId = "134"
        val bytes = fileSerializer.toBytes(fileId, weirdString)
        val restoredString = fileSerializer.toString(fileId, bytes)
        assertEquals(weirdString, restoredString)
    }

    @Test
    fun `serialize and deserialize should be consistent when compressed`() {
        val weirdString = "The quick коричневая 𓃥 jumps over the lazy 𓊃𓍿𓅓𓃡"
        val fileId = "134"
        val bytes = fileSerializer.toBytes(fileId, weirdString)
        val restoredString = fileSerializer.toString(fileId, bytes)
        assertEquals(weirdString, restoredString)
    }
}
