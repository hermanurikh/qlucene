package com.qbutton.qlucene

import com.qbutton.qlucene.common.IndexCanceller
import com.qbutton.qlucene.common.Resettable
import com.qbutton.qlucene.dto.Sentence
import com.qbutton.qlucene.dto.Word
import com.qbutton.qlucene.searcher.SearchFacade
import com.qbutton.qlucene.updater.FileRegistrationFacade
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * A class exposing all API via which a user interacts with application.
 */

class UserAPI : AutoCloseable {
    private val searchFacade: SearchFacade
    private val indexCanceller: IndexCanceller
    private val statefulBeans: List<Resettable>
    private val fileRegistrationFacade: FileRegistrationFacade

    init {
        val context: ApplicationContext = AnnotationConfigApplicationContext("com.qbutton.qlucene")
        searchFacade = context.getBean("searchFacade") as SearchFacade
        indexCanceller = context.getBean("indexCanceller") as IndexCanceller
        statefulBeans = context.getBeansOfType(Resettable::class.java).values.toList()
        fileRegistrationFacade = context.getBean("fileRegistrationFacade") as FileRegistrationFacade
    }

    /**
     * Adds a single file.
     *
     * Paths can be both relative and absolute.
     * E.g.
     *  src/test/resources/testfiles/rootdir/nesteddir/simpleFile2.txt
     *  src
     *  /Users/gurikh/code/intellij-community-master
     */
    fun addToIndex(path: String) = fileRegistrationFacade.register(path)

    /**
     * Removes path from index.
     */
    fun removeFromIndex(path: String) = fileRegistrationFacade.unregister(path)

    /**
     * Searches for given word.
     */
    fun searchWord(token: String) = searchFacade.search(Word(token))

    /**
     * Searches for given sentence.
     */
    fun searchSentence(token: String) = searchFacade.search(Sentence(token))

    /**
     * Cancels indexing given path. It is done to urgently stop current indexing which may be traversing some enormous
     * file tree.
     *
     * If no indexing is currently happening for that path, this will cancel next indexing attempt for it.
     *
     * It may be racy, if cancelling of some path intersects with another indexing in that path (e.g. we start indexing
     * path A, we start indexing path B which is a subtree of A, then we cancel A), and is therefore not thread-safe.
     *
     */
    fun cancelIndexing(path: String) = indexCanceller.cancel(path)

    /**
     * Clears current indices and resets state. This method is not thread-safe and is made mostly for ease of testing.
     */
    fun resetState() {
        statefulBeans.forEach { it.resetState() }
    }

    override fun close() {
        statefulBeans.forEach { it.close() }
    }
}
