package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest

/**
 * This test checks operations which are triggered by using UserAPI, e.g. direct addition to index.
 */
@SpringBootTest
class IndexAdditionTest {

    @Autowired
    private lateinit var userAPI: UserAPI

    @Value("\${reducer.size-based.max-size}")
    private var maxOutputSize: Int = 0

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

        filesFound = userAPI.searchSentence("Simple sentence 2...")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])
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

    @Test
    fun `adding multiple unrelated directories should add all of them to index`() {
        // given
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
        filesFound = userAPI.searchWord("january")
        assertTrue(filesFound.isEmpty())
        filesFound = userAPI.searchWord("devils")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(rootDir)
        userAPI.addToIndex(rootDir2)

        // then
        filesFound = userAPI.searchWord("word3")
        assertEquals(1, filesFound.size)
        assertEquals("$rootDir2/simpleFile3.txt", filesFound[0])
        filesFound = userAPI.searchWord("august")
        assertEquals(2, filesFound.size)
        assertAll(
            { filesFound.contains(nestedFile) },
            { filesFound.contains("$rootDir2/simpleFile3.txt") }
        )
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$rootDir/simpleFile1.txt", filesFound[0])
        filesFound = userAPI.searchWord("devils")
        assertEquals(4, filesFound.size)
        assertAll(
            { filesFound.contains("$rootDir/simpleFile1.txt") },
            { filesFound.contains("$rootDir/englishWords1.txt") },
            { filesFound.contains("$rootDir/englishWords2.txt") },
            { filesFound.contains("$rootDir2/simpleFile3.txt") }
        )
    }

    @Test
    fun `searching for a term should list files ordered by number of term occurrences`() {
        // given
        var filesFound = userAPI.searchWord("april")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(rootDir3)

        // then
        filesFound = userAPI.searchWord("april")
        assertEquals(3, filesFound.size)
        assertEquals("$rootDir3/file3.txt", filesFound[0])
        assertEquals("$rootDir3/file1.txt", filesFound[1])
        assertEquals("$rootDir3/file2.txt", filesFound[2])
    }

    @Test
    fun `searching for a term should output limited number of documents`() {
        // given
        var filesFound = userAPI.searchWord("december")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(rootDir4)

        // then
        filesFound = userAPI.searchWord("december")
        assertEquals(maxOutputSize, filesFound.size)
    }
}
