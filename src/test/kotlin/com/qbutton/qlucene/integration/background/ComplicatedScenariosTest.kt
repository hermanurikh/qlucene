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
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.FileSystemUtils
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This test checks that background events triggered in more complicated scenarios than direct addition/removal are handled properly.
 */
@SpringBootTest
class ComplicatedScenariosTest {

    @Autowired
    private lateinit var userAPI: UserAPI

    private val fileToMonitor = "$tmpTestNestedDir/$nestedFileName"

    @BeforeEach
    fun createDirs() {
        removeFilesAndDirs()
        val path = Paths.get(tmpTestNestedDir)
        Files.createDirectories(path)
        Files.copy(Paths.get(nestedFile), Paths.get(fileToMonitor))
        userAPI.resetState()
    }

    @Test
    fun `when a file is deleted, and then added back, it is properly re-indexed`() {
        // given [file was indexed and then deleted]
        ensureFileIsSearchable(fileToMonitor)
        Files.delete(Paths.get(fileToMonitor))
        Thread.sleep(eventsRetrievalDelay)
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when [we add it back]
        Files.copy(Paths.get(nestedFile), Paths.get(fileToMonitor))
        Thread.sleep(eventsRetrievalDelay)

        // then [contents should be there again]
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(fileToMonitor, filesFound[0])
    }

    @Test
    fun `when a file is deleted as a part of dir, and then added back, its contents are properly re-indexed`() {
        // given [file was indexed as a part of dir, then dir has been deleted]
        ensureFileIsSearchable(tmpTestDir)
        val enclosingDir = Paths.get(tmpTestNestedDir)
        FileSystemUtils.deleteRecursively(enclosingDir)
        Thread.sleep(eventsRetrievalDelay)
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when [we add directory and file back]
        Files.createDirectories(enclosingDir)
        Files.copy(Paths.get(nestedFile), Paths.get(fileToMonitor))
        Thread.sleep(eventsRetrievalDelay)

        // then [contents should be there again]
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(fileToMonitor, filesFound[0])
    }

    @Test
    fun `when several file modifications happen, eventual result should be consistent`() {
        // given
        ensureFileIsSearchable(fileToMonitor)

        // when
        Files.write(Paths.get(fileToMonitor), "word one".toByteArray())
        Files.write(Paths.get(fileToMonitor), "two words".toByteArray())
        Files.write(Paths.get(fileToMonitor), "three words".toByteArray())
        Files.write(Paths.get(fileToMonitor), "four sentence".toByteArray())
        Thread.sleep(eventsRetrievalDelay)

        // then
        assertAll(
            {
                val filesFound = userAPI.searchWord("four")
                assertEquals(1, filesFound.size)
                assertEquals(fileToMonitor, filesFound[0])
            },
            {
                val filesFound = userAPI.searchWord("sentence")
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
