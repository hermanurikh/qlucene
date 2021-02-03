package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * This test checks removal from index.
 */
class IndexRemovalTest {

    private val userAPI = UserAPI()

    @BeforeEach
    fun clearState() {
        userAPI.resetState()
    }

    @Test
    fun `file should not be searchable after it is registered directly and unregistered directly`() {
        // given
        val filePath = nestedFile.toAbsolutePath()
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        userAPI.addToIndex(filePath)
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])

        // when
        userAPI.removeFromIndex(filePath)

        // then
        filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `file should not be searchable after it is registered as a part of dir and unregistered directly`() {
        // given
        val filePath = nestedFile.toAbsolutePath()
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        userAPI.addToIndex(nestedDir)
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])

        // when
        userAPI.removeFromIndex(filePath)

        // then
        filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `file should not be searchable after it is added as a part of nested dir, then nested dir is unregistered`() {
        // given
        val dirPath = rootDir
        val filePath = nestedFile.toAbsolutePath()
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        userAPI.addToIndex(dirPath)
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])

        // when
        userAPI.removeFromIndex(dirPath)

        // then
        filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `file should not be searchable after it is registered as a part of dir, then dir is unregistered`() {
        // given
        val filePath = nestedFile.toAbsolutePath()
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        userAPI.addToIndex(nestedDir)
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])

        // when
        userAPI.removeFromIndex(nestedDir)

        // then
        filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }
}
