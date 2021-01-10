package com.qbutton.qlucene

import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.dto.AbnormalFileRegistrationResult
import com.qbutton.qlucene.dto.FileNotFoundRegistrationResult
import com.qbutton.qlucene.dto.RegistrationResult
import com.qbutton.qlucene.dto.Sentence
import com.qbutton.qlucene.dto.Word
import com.qbutton.qlucene.searcher.SearchFacade
import com.qbutton.qlucene.updater.background.WatchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths

/**
 * A class exposing all API via which a user interacts with application.
 *
 * A super-plain rest controller with methods and examples to hit them using curl.
 */
@RestController
class UserAPI @Autowired constructor(
    private val searchFacade: SearchFacade,
    private val watchService: WatchService,
    private val statefulBeans: List<Resettable>
) {
    /**
     * E.g.
     *  - add a single file:
     *      curl --data "path=src/test/resources/testfiles/rootdir/nesteddir/simpleFile2.txt" http://localhost:8077/add/
     *  - add a directory (recursively, with subdirectories):
     *      curl --data "path=src/test/resources/testfiles" http://localhost:8077/add/
     */
    @PostMapping("/add/")
    fun addToIndex(@RequestParam path: String): RegistrationResult {
        val file = Paths.get(path).toFile()

        return when {
            !file.exists() -> FileNotFoundRegistrationResult(path)
            file.isDirectory -> {
                watchService.registerRootDir(path)
            }
            file.isFile -> {
                watchService.registerFile(path)
            }
            else -> AbnormalFileRegistrationResult(path)
        }
    }

    /**
     * E.g.
     * curl -i http://localhost:8077/search/word/august
     */
    @GetMapping("/search/word/{token}")
    fun searchWord(@PathVariable token: String) = searchFacade.search(Word(token))
    /**
     * E.g.
     * curl -i http://localhost:8077/search/sentence/Simple%20sentence%202...
     */
    @GetMapping("/search/sentence/{token}")
    fun searchSentence(@PathVariable token: String) = searchFacade.search(Sentence(token))

    /**
     * Clears current indices and resets state. This method is not thread-safe and is made mostly for ease of testing.
     */
    @GetMapping("/reset/")
    fun resetState() {
        statefulBeans.forEach { it.resetState() }
    }
}
