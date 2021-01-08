package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
class FilesDeletionTest {

    private val fileToDeletePath = "$tmpTestNestedDir/$nestedFileName"

    @Autowired
    private lateinit var userAPI: UserAPI

    @BeforeEach
    fun createFilesAndDirs() {
        val path = Paths.get(tmpTestNestedDir)
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
        val pathFrom = Paths.get(nestedFile)
        val pathTo = Paths.get(fileToDeletePath)
        if (!Files.exists(pathTo)) {
            Files.copy(pathFrom, pathTo)
        }
    }

    @Disabled(
        """
    This test uses Thread.sleep to wait for background event to come which may be not the best practices. 
    Also, it slows
    down the build time dramatically.
    It still works though, so enable it if you want to check this functionality.
    """
    )
    @Test
    fun `file should not be searchable after it is deleted directly`() {
        // given
        ensureFileIsSearchable()

        // when
        Files.delete(Paths.get(fileToDeletePath))
        Thread.sleep(15000)

        // then
        // index should not contain the terms any more
        val filesFound = userAPI.searchWord("august")
        assertTrue(filesFound.isEmpty())
    }

    @Disabled(
        """
    In Java, deleting a dir requires prior deleting of all files. So not clear how to test deleting the enclosing dir 
    without triggering file deletion event first.
        """
    )
    @Test
    fun `file should not be searchable after it is deleted as part of dir`() {
    }

    @Disabled(
        """
    In Java, deleting a dir requires prior deleting of all files. So not clear how to test deleting the enclosing dir 
    without triggering file deletion event first.
        """
    )
    @Test
    fun `file should not be searchable after it is deleted as part of nested dir`() {
    }

    private fun ensureFileIsSearchable() {
        userAPI.addToIndex(fileToDeletePath)
        val filesFound = userAPI.searchWord("august")
        assertEquals(1, filesFound.size)
        assertEquals(fileToDeletePath, filesFound[0])
    }

    @AfterEach
    fun removeFilesAndDirs() {
        for (path in listOf(tmpTestNestedDir, tmpTestDir, tmpDir)) {
            deleteDir(path)
        }
    }

    private fun deleteDir(path: String) {
        val rootPath = Paths.get(path)
        val contents = rootPath.toFile().listFiles()
        if (contents != null) {
            for (file in contents) {
                Files.delete(file.toPath())
            }
        }
        Files.delete(rootPath)
    }
}
