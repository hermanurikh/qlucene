package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * This test checks operations which are triggered by using UserAPI, e.g. direct addition to index.
 */
@SpringBootTest
class IndexAdditionTest {

    @Autowired
    private lateinit var userAPI: UserAPI

    @BeforeEach
    fun clearState() {
        userAPI.resetState()
    }

    @Test
    fun `file should be searchable after it is added directly`() {
        // given
        val filePath = nestedFile
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(filePath)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])

        filesFound = userAPI.searchWord("january")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `file should be searchable after it is added as a part of dir`() {
        // given
        val dirPath = nestedDir
        val filePath = nestedFile
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(dirPath)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])

        filesFound = userAPI.searchWord("january")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `file should be searchable after it is added as a part of nested dir`() {
        // given
        val dirPath = rootDir
        val filePath = nestedFile
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(dirPath)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])
    }

    @Test
    fun `dir should add files of different nestedness level to index`() {
        // given
        val dirPath = rootDir
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(dirPath)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(nestedFile, filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$rootDir/simpleFile1.txt", filesFound[0])
    }

    @Test
    fun `adding file, then dir should add different files at different times`() {
        // given
        val filePath = nestedFile
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        filesFound = userAPI.searchWord("january")
        assertTrue(filesFound.isEmpty())
        filesFound = userAPI.searchWord("devils")
        assertTrue(filesFound.isEmpty())

        // when we add only one file, we find only it's contents
        userAPI.addToIndex(filePath)
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(nestedFile, filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertTrue(filesFound.isEmpty())
        filesFound = userAPI.searchWord("devils")
        assertTrue(filesFound.isEmpty())

        // when afterwards we add a top-level directory, it adds other files, but current file is still there
        userAPI.addToIndex(rootDir)
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(nestedFile, filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$rootDir/simpleFile1.txt", filesFound[0])
        filesFound = userAPI.searchWord("devils")
        assertEquals(3, filesFound.size)
        assertAll(
            { filesFound.contains("$rootDir/simpleFile1.txt") },
            { filesFound.contains("$rootDir/englishWords1.txt") },
            { filesFound.contains("$rootDir/englishWords2.txt") }
        )
    }
}