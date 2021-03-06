package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import com.qbutton.qlucene.dto.FileSizeExceedsLimits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * This test checks operations which are triggered by using UserAPI, e.g. direct addition to index.
 */
class IndexAdditionTest {

    private var userAPI = UserAPI()

    private var maxOutputSize: Int = 20

    @BeforeEach
    fun clearState() {
        userAPI.resetState()
    }

    @Test
    fun `file should be searchable after it is added directly`() {
        // given
        val filePath = nestedFile.toAbsolutePath()
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
        val filePath = nestedFile.toAbsolutePath()
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
        val filePath = nestedFile.toAbsolutePath()
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
        assertEquals(nestedFile.toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$rootDir${fileSeparator}simpleFile1.txt".toAbsolutePath(), filesFound[0])
    }

    @Test
    fun `adding file, then dir should add different files at different times`() {
        // given
        val filePath = nestedFile.toAbsolutePath()
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
        assertEquals(filePath, filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertTrue(filesFound.isEmpty())
        filesFound = userAPI.searchWord("devils")
        assertTrue(filesFound.isEmpty())

        // when afterwards we add a top-level directory, it adds other files, but current file is still there
        userAPI.addToIndex(rootDir)
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(filePath, filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$rootDir${fileSeparator}simpleFile1.txt".toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("devils")
        assertEquals(3, filesFound.size)
        assertAll(
            { filesFound.contains("$rootDir${fileSeparator}simpleFile1.txt".toAbsolutePath()) },
            { filesFound.contains("$rootDir${fileSeparator}englishWords1.txt".toAbsolutePath()) },
            { filesFound.contains("$rootDir${fileSeparator}englishWords2.txt".toAbsolutePath()) }
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
        assertEquals("$rootDir2${fileSeparator}simpleFile3.txt".toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("august")
        assertEquals(2, filesFound.size)
        assertAll(
            { filesFound.contains(nestedFile.toAbsolutePath()) },
            { filesFound.contains("$rootDir2${fileSeparator}simpleFile3.txt".toAbsolutePath()) }
        )
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("$rootDir${fileSeparator}simpleFile1.txt".toAbsolutePath(), filesFound[0])
        filesFound = userAPI.searchWord("devils")
        assertEquals(4, filesFound.size)
        assertAll(
            { filesFound.contains("$rootDir${fileSeparator}simpleFile1.txt".toAbsolutePath()) },
            { filesFound.contains("$rootDir${fileSeparator}englishWords1.txt".toAbsolutePath()) },
            { filesFound.contains("$rootDir${fileSeparator}englishWords2.txt".toAbsolutePath()) },
            { filesFound.contains("$rootDir2${fileSeparator}simpleFile3.txt".toAbsolutePath()) }
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
        assertEquals("$rootDir3${fileSeparator}file3.txt".toAbsolutePath(), filesFound[0])
        assertEquals("$rootDir3${fileSeparator}file1.txt".toAbsolutePath(), filesFound[1])
        assertEquals("$rootDir3${fileSeparator}file2.txt".toAbsolutePath(), filesFound[2])
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
        assertTrue(filesFound.size <= maxOutputSize)
    }

    @Test
    fun `adding a directory won't result in indexing file deeper than configured maxDepth`() {
        // given
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(rootDir6)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(
            (
                "$rootDir6$fileSeparator" +
                    "l2${fileSeparator}l3${fileSeparator}l4${fileSeparator}l5${fileSeparator}l6$fileSeparator" +
                    "l7${fileSeparator}l8${fileSeparator}l9${fileSeparator}l10${fileSeparator}l11$fileSeparator" +
                    "l12${fileSeparator}l13${fileSeparator}l14$fileSeparator" +
                    "l15${fileSeparator}file1.txt"
                ).toAbsolutePath(),
            filesFound[0]
        )
    }

    @Test
    fun `adding file which exceeds configured limits will not result in indexing`() {
        // given
        var filesFound = userAPI.searchWord("zoo")
        assertTrue(filesFound.isEmpty())

        // when
        val additionResult = userAPI.addToIndex("$rootDir${fileSeparator}$bigFileName")

        // then
        assertTrue(additionResult is FileSizeExceedsLimits)
        filesFound = userAPI.searchWord("zoo")
        assertTrue(filesFound.isEmpty())
    }

    @Test
    fun `adding file with unsupported format will not result in indexing`() {
        // given

        // when
        userAPI.addToIndex(rootDir5)

        // then
        val filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }
}
