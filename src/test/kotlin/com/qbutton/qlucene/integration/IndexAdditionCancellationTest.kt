package com.qbutton.qlucene.integration

import com.qbutton.qlucene.UserAPI
import com.qbutton.qlucene.dto.DirectoryRegistrationCancelled
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * This test checks additions which are cancelled.
 */
@SpringBootTest
class IndexAdditionCancellationTest {

    @Autowired
    private lateinit var userAPI: UserAPI

    @BeforeEach
    fun clearState() {
        userAPI.resetState()
    }

    @Test
    fun `file should not be accessible if it has been indexed, but also cancelled`() {
        // given
        val filePath = "src".toAbsolutePath()
        var filesFound = userAPI.searchWord("class")
        assertTrue(filesFound.isEmpty())

        // when
        val countDownLatch = CountDownLatch(1)
        val future = Executors.newFixedThreadPool(1)
            .submit(
                Callable {
                    countDownLatch.countDown()
                    val res = userAPI.addToIndex(filePath)
                    res
                }
            )
        // await to let the thread start doing things, only then cancel
        countDownLatch.await()
        userAPI.cancelIndexing(filePath)
        val regResult = future.get()

        // then
        assertTrue(regResult is DirectoryRegistrationCancelled)
        filesFound = userAPI.searchWord("class")
        assertTrue(filesFound.isEmpty())
    }
}
