package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode

@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class UserAPITest {

    @Autowired private lateinit var userAPI: UserAPI

    @Test
    fun `file should be searchable after it is added directly`() {
        // given
        val filePath = "src/test/resources/level1dir_1/level2dir_1/simpleFile2.txt"
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
        val dirPath = "src/test/resources/level1dir_1/level2dir_1"
        val filePath = "src/test/resources/level1dir_1/level2dir_1/simpleFile2.txt"
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
        val dirPath = "src/test/resources/level1dir_1"
        val filePath = "src/test/resources/level1dir_1/level2dir_1/simpleFile2.txt"
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
        val dirPath = "src/test/resources/level1dir_1"
        var filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())

        // when
        userAPI.addToIndex(dirPath)

        // then
        filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals("src/test/resources/level1dir_1/level2dir_1/simpleFile2.txt", filesFound[0])
        filesFound = userAPI.searchWord("january")
        assertEquals(1, filesFound.size)
        assertEquals("src/test/resources/level1dir_1/simpleFile1.txt", filesFound[0])
    }
}
