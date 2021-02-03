package com.qbutton.qlucene.integration.background

import com.qbutton.qlucene.UserAPI
import com.qbutton.qlucene.integration.fileSeparator
import com.qbutton.qlucene.integration.nestedFile
import com.qbutton.qlucene.integration.nestedFileName
import com.qbutton.qlucene.integration.tmpDir
import com.qbutton.qlucene.integration.tmpTestNestedDir
import com.qbutton.qlucene.integration.toAbsolutePath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.util.FileSystemUtils
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This test checks that file modification events are caught and handled properly.
 */
class FilesModificationTest {

    private val userAPI = UserAPI()

    private val fileToMonitor = "$tmpTestNestedDir${fileSeparator}$nestedFileName".toAbsolutePath()

    @BeforeEach
    fun createDirs() {
        removeFilesAndDirs()
        val path = Paths.get(tmpTestNestedDir)
        Files.createDirectories(path)
        Files.copy(Paths.get(nestedFile), Paths.get(fileToMonitor))
        userAPI.resetState()
    }

    @Test
    fun `when file is monitored, modifying the file should be reflected in index`() {
        // given
        ensureFileIsSearchable(fileToMonitor)
        val content = "several words"

        // when
        Files.write(Paths.get(fileToMonitor), content.toByteArray())
        Thread.sleep(eventsRetrievalDelay)

        // then
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        assertAll(
            {
                filesFound = userAPI.searchWord("several")
                assertEquals(1, filesFound.size)
                assertEquals(fileToMonitor, filesFound[0])
            },
            {
                filesFound = userAPI.searchWord("words")
                assertEquals(1, filesFound.size)
                assertEquals(fileToMonitor, filesFound[0])
            }
        )
    }

    @Test
    fun `when enclosing dir is monitored, modifying the file should be reflected in index`() {
        // given
        ensureFileIsSearchable(tmpTestNestedDir)
        val content = "several words"

        // when
        Files.write(Paths.get(fileToMonitor), content.toByteArray())
        Thread.sleep(eventsRetrievalDelay)

        // then
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        assertAll(
            {
                filesFound = userAPI.searchWord("several")
                assertEquals(1, filesFound.size)
                assertEquals(fileToMonitor, filesFound[0])
            },
            {
                filesFound = userAPI.searchWord("words")
                assertEquals(1, filesFound.size)
                assertEquals(fileToMonitor, filesFound[0])
            }
        )
    }

    @Test
    fun `when grandGrandParent dir is monitored, modifying the file should be reflected in index`() {
        // given
        ensureFileIsSearchable(tmpDir)
        val content = "several words"

        // when
        Files.write(Paths.get(fileToMonitor), content.toByteArray())
        Thread.sleep(eventsRetrievalDelay)

        // then
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        assertAll(
            {
                filesFound = userAPI.searchWord("several")
                assertEquals(1, filesFound.size)
                assertEquals(fileToMonitor, filesFound[0])
            },
            {
                filesFound = userAPI.searchWord("words")
                assertEquals(1, filesFound.size)
                assertEquals(fileToMonitor, filesFound[0])
            }
        )
    }

    @AfterEach
    fun removeFilesAndDirs() {
        FileSystemUtils.deleteRecursively(Paths.get(tmpDir))
    }

    private fun ensureFileIsSearchable(pathToAdd: String) {
        userAPI.addToIndex(pathToAdd)
        val filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(fileToMonitor, filesFound[0])
    }
}
