package com.qbutton.qlucene.integration.background

import com.qbutton.qlucene.UserAPI
import com.qbutton.qlucene.integration.fileSeparator
import com.qbutton.qlucene.integration.nestedDirName
import com.qbutton.qlucene.integration.nestedFile
import com.qbutton.qlucene.integration.nestedFileName
import com.qbutton.qlucene.integration.rootDir
import com.qbutton.qlucene.integration.tmpDir
import com.qbutton.qlucene.integration.tmpTestDir
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
 * This test checks that background events related to file addition are caught and handled properly.
 */
class FilesAdditionTest {

    private val userAPI = UserAPI()

    @BeforeEach
    fun createDirs() {
        removeFilesAndDirs()
        val path = Paths.get(tmpTestNestedDir)
        Files.createDirectories(path)
        userAPI.resetState()
    }

    @Test
    fun `when dir is monitored, adding file there should add it to index`() {
        // given
        userAPI.addToIndex(tmpTestDir)
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        val fileFrom = nestedFile
        val fileTo = "$tmpTestDir${fileSeparator}$nestedFileName".toAbsolutePath()

        // when
        Files.copy(Paths.get(fileFrom), Paths.get(fileTo))
        Thread.sleep(eventsRetrievalDelay)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(fileTo, filesFound[0])
    }

    @Test
    fun `when dir is monitored, adding file to a nested dir should add it to index`() {
        // given
        userAPI.addToIndex(tmpTestDir)
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        val fileFrom = nestedFile
        val fileTo = "$tmpTestNestedDir${fileSeparator}$nestedFileName".toAbsolutePath()

        // when
        Files.copy(Paths.get(fileFrom), Paths.get(fileTo))
        Thread.sleep(eventsRetrievalDelay)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(fileTo, filesFound[0])
    }

    @Test
    fun `when dir is monitored, adding a dir to it with several files should add all of them to index`() {
        // given
        val dirFrom = rootDir
        val dirTo = "$tmpTestNestedDir${fileSeparator}innerDir"
        userAPI.addToIndex(tmpTestNestedDir)
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        FileSystemUtils.copyRecursively(Paths.get(dirFrom), Paths.get(dirTo))
        Thread.sleep(eventsRetrievalDelay)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals("$dirTo${fileSeparator}$nestedDirName${fileSeparator}$nestedFileName".toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$dirTo${fileSeparator}simpleFile1.txt".toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("devils")
        assertEquals(3, filesFound.size)
        assertAll(
            { filesFound.contains("$dirTo${fileSeparator}simpleFile1.txt".toAbsolutePath()) },
            { filesFound.contains("$dirTo${fileSeparator}englishWords1.txt".toAbsolutePath()) },
            { filesFound.contains("$dirTo${fileSeparator}englishWords2.txt".toAbsolutePath()) }
        )
    }

    @Test
    fun `when dir is monitored, adding a dir to nested internal dir with several files should add all of them to index`() {
        // given
        val dirFrom = rootDir
        val dirTo = "$tmpTestNestedDir${fileSeparator}innerDir"
        userAPI.addToIndex(tmpTestDir)
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        FileSystemUtils.copyRecursively(Paths.get(dirFrom), Paths.get(dirTo))
        Thread.sleep(eventsRetrievalDelay)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals("$dirTo${fileSeparator}$nestedDirName/$nestedFileName".toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$dirTo${fileSeparator}simpleFile1.txt".toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("devils")
        assertEquals(3, filesFound.size)
        assertAll(
            { filesFound.contains("$dirTo${fileSeparator}simpleFile1.txt".toAbsolutePath()) },
            { filesFound.contains("$dirTo${fileSeparator}englishWords1.txt".toAbsolutePath()) },
            { filesFound.contains("$dirTo${fileSeparator}englishWords2.txt".toAbsolutePath()) }
        )
    }

    @AfterEach
    fun removeFilesAndDirs() {
        FileSystemUtils.deleteRecursively(Paths.get(tmpDir))
    }
}

/**
 * Sleep this much before checking whether event has been received. Yeah, this may be not the greatest idea, not sure
 * how to do it better though.
 */
const val eventsRetrievalDelay = 15_000L
