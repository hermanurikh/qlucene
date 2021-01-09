package com.qbutton.qlucene.integration.background

import com.qbutton.qlucene.UserAPI
import com.qbutton.qlucene.integration.nestedFile
import com.qbutton.qlucene.integration.nestedFileName
import com.qbutton.qlucene.integration.tmpDir
import com.qbutton.qlucene.integration.tmpTestDir
import com.qbutton.qlucene.integration.tmpTestNestedDir
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.FileSystemUtils
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This test checks that background events related to file deletion are caught and handled properly.
 */
@SpringBootTest
class FilesDeletionTest {

    private val fileToDeletePath = "$tmpTestNestedDir/$nestedFileName"

    @Autowired
    private lateinit var userAPI: UserAPI

    @BeforeEach
    fun createFilesAndDirs() {
        removeFilesAndDirs()
        val path = Paths.get(tmpTestNestedDir)
        Files.createDirectories(path)
        val pathFrom = Paths.get(nestedFile)
        val pathTo = Paths.get(fileToDeletePath)
        Files.copy(pathFrom, pathTo)
        userAPI.resetState()
    }

    @Test
    fun `file should not be searchable after it is deleted directly`() {
        // given
        ensureFileIsSearchable(fileToDeletePath)

        // when
        Files.delete(Paths.get(fileToDeletePath))
        // yeah, this is bad, that's why these tests are disabled by default in build.gradle
        Thread.sleep(eventsRetrievalDelay)

        // then
        // index should not contain the terms any more
        val filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `file should not be searchable after it is deleted as part of dir`() {
        // given
        ensureFileIsSearchable(tmpTestDir)

        // when
        FileSystemUtils.deleteRecursively(Paths.get(tmpTestNestedDir))
        Thread.sleep(eventsRetrievalDelay)

        // then
        // index should not contain the terms any more
        val filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `file should not be searchable after it is deleted as part of nested dir`() {
        // given
        ensureFileIsSearchable(tmpDir)

        // when
        FileSystemUtils.deleteRecursively(Paths.get(tmpTestDir))
        Thread.sleep(eventsRetrievalDelay)

        // then
        // index should not contain the terms any more
        val filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }

    private fun ensureFileIsSearchable(path: String) {
        userAPI.addToIndex(path)
        val filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(fileToDeletePath, filesFound[0])
    }

    @AfterEach
    fun removeFilesAndDirs() {
        FileSystemUtils.deleteRecursively(Paths.get(tmpDir))
    }
}
