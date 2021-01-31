package com.qbutton.qlucene.common

import com.qbutton.qlucene.integration.nestedDir
import com.qbutton.qlucene.integration.nestedFile
import com.qbutton.qlucene.integration.resourcesRoot
import com.qbutton.qlucene.integration.rootDir
import com.qbutton.qlucene.integration.rootDir2
import com.qbutton.qlucene.integration.rootDir3
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class FilteredOutRootsTest {

    private val registeredRoots = RegisteredRoots(FileIdConverter())
    private val filteredOutRoots = FilteredOutRoots(registeredRoots)

    @BeforeEach
    fun clearState() {
        registeredRoots.resetState()
        filteredOutRoots.resetState()
    }

    @Test
    fun `should filter out when path is the same as filtered out root`() {
        // given
        filteredOutRoots.add(Paths.get(resourcesRoot))

        // when
        val shouldFilterOut = filteredOutRoots.shouldFilterOut(resourcesRoot)

        // then
        assertTrue(shouldFilterOut)
    }

    @Test
    fun `should filter out when path is part of filtered out root`() {
        // given
        filteredOutRoots.add(Paths.get(resourcesRoot))

        // when
        val shouldFilterOut = filteredOutRoots.shouldFilterOut(rootDir)

        // then
        assertTrue(shouldFilterOut)
    }

    @Test
    fun `should not filter out when filtered out roots are empty`() {
        // given

        // when
        val shouldFilterOut = filteredOutRoots.shouldFilterOut(rootDir)

        // then
        assertFalse(shouldFilterOut)
    }

    @Test
    fun `should not filter out when path is different from the ones in filtered out root`() {
        // given
        filteredOutRoots.add(Paths.get(rootDir2))

        // when
        val shouldFilterOut = filteredOutRoots.shouldFilterOut(rootDir3)

        // then
        assertFalse(shouldFilterOut)
    }

    @Test
    fun `should filter out when filtered out root is the same as registered root`() {
        // given
        filteredOutRoots.add(Paths.get(rootDir2))
        registeredRoots.add(Paths.get(rootDir2))

        // when
        val shouldFilterOut = filteredOutRoots.shouldFilterOut(rootDir2)

        // then
        assertTrue(shouldFilterOut)
    }

    @Test
    fun `should filter out when filtered out root is earlier than registered root`() {
        // given
        filteredOutRoots.add(Paths.get(nestedDir))
        registeredRoots.add(Paths.get(rootDir))

        // when
        val shouldFilterOut = filteredOutRoots.shouldFilterOut(nestedFile)

        // then
        assertTrue(shouldFilterOut)
    }

    @Test
    fun `should not filter out when filtered out root is later than registered root`() {
        // given
        filteredOutRoots.add(Paths.get(rootDir))
        registeredRoots.add(Paths.get(nestedDir))

        // when
        val shouldFilterOut = filteredOutRoots.shouldFilterOut(nestedFile)

        // then
        assertFalse(shouldFilterOut)
    }
}
