package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class FilesAdditionTest {

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
        val dirPath = level2Dir
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
        val dirPath = level1Dir
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
        val dirPath = level1Dir
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
        assertEquals("src/test/resources/level1dir_1/simpleFile1.txt", filesFound[0])
    }
}
