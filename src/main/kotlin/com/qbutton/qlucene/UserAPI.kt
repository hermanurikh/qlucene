package com.qbutton.qlucene

import com.qbutton.qlucene.common.IndexCanceller
import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.dto.Sentence
import com.qbutton.qlucene.dto.Word
import com.qbutton.qlucene.searcher.SearchFacade
import com.qbutton.qlucene.updater.FileRegistrationFacade
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * A class exposing all API via which a user interacts with application.
 *
 * A super-plain rest controller with methods and examples to hit them using curl.
 */
@RestController
class UserAPI @Autowired constructor(
    private val searchFacade: SearchFacade,
    private val indexCanceller: IndexCanceller,
    private val statefulBeans: List<Resettable>,
    private val fileRegistrationFacade: FileRegistrationFacade,
) {
    /**
     * E.g.
     *  - add a single file:
     *      curl --data "path=src/test/resources/testfiles/rootdir/nesteddir/simpleFile2.txt" http://localhost:8077/add/
     *  - add a directory (recursively, with subdirectories):
     *      curl --data "path=src" http://localhost:8077/add/
     *      curl --data "path=/Users/gurikh/code/intellij-community-master" http://localhost:8077/add/
     */
    @PostMapping("/add/")
    fun addToIndex(@RequestParam path: String) = fileRegistrationFacade.register(path)

    /**
     * E.g.
     * curl --data "path=src/test/resources/testfiles" http://localhost:8077/remove/
     */
    @PostMapping("/remove/")
    fun removeFromIndex(@RequestParam path: String) = fileRegistrationFacade.unregister(path)

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
     * Cancels indexing given path. It is done to urgently stop current indexing which may be traversing some enormous
     * file tree.
     *
     * If no indexing is currently happening for that path, this will cancel next indexing attempt for it.
     *
     * It may be racy, if cancelling of some path intersects with another indexing in that path, and is therefore
     * not thread-safe.
     *
     * E.g.
     * curl --data "path=/Users/gurikh/code/intellij-community-master" http://localhost:8077/cancel/
     */
    @PostMapping("/cancel/")
    fun cancelIndexing(@RequestParam path: String) = indexCanceller.cancel(path)

    /**
     * Clears current indices and resets state. This method is not thread-safe and is made mostly for ease of testing.
     */
    @GetMapping("/reset/")
    fun resetState() {
        statefulBeans.forEach { it.resetState() }
    }
}
