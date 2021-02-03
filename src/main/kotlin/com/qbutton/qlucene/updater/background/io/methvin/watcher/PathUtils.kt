package com.qbutton.qlucene.updater.background.io.methvin.watcher

import io.methvin.watcher.hashing.FileHash
import io.methvin.watcher.hashing.FileHasher
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.util.SortedMap

/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

object PathUtils {

    private val logger = LoggerFactory.getLogger(PathUtils::class.java)

    fun hash(fileHasher: FileHasher, path: Path?): FileHash? {
        return try {
            if (Files.isDirectory(path)) {
                FileHash.directory()
            } else {
                if (!Files.exists(path)) {
                    null
                } else fileHasher.hash(path)
            }
        } catch (e: IOException) {
            null
        }
    }

    fun <T> subMap(pathMap: SortedMap<Path?, T>, treeRoot: Path?): SortedMap<Path?, T> {
        return pathMap.subMap(treeRoot, Paths.get(treeRoot.toString(), "" + Character.MAX_VALUE))
    }

    fun initWatcherState(
        root: Path,
        fileHasher: FileHasher,
        hashes: MutableMap<Path?, FileHash?>,
        directories: MutableSet<Path?>,
        maxDepth: Int,
        registeredFileIds: MutableSet<String>,
        toFileIdAction: (Path) -> String,
        isTraversalCancelledForId: (String) -> Boolean,
        indexFileAction: (Path) -> Unit,
    ) {
        val addHash = { path: Path ->
            val hash = hash(fileHasher, path)
            if (hash != null) hashes[path] = hash
        }
        val rootPathId = toFileIdAction(root.toAbsolutePath())
        recursiveVisitFiles(
            root,
            {
                directories.add(it)
                addHash(it)
                !isTraversalCancelledForId(rootPathId)
            },
            {
                addHash(it)
                if (isTraversalCancelledForId(rootPathId)) {
                    false
                } else {
                    indexFileAction(it)
                    registeredFileIds.add(toFileIdAction(it.toAbsolutePath()))
                    true
                }
            },
            maxDepth
        )
    }

    fun recursiveVisitFiles(
        file: Path?,
        onDirectory: (Path) -> Boolean,
        onFile: (Path) -> Boolean,
        maxDepth: Int,
    ) {
        Files.walkFileTree(
            file,
            emptySet(),
            maxDepth,
            object : SimpleFileVisitor<Path>() {

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return if (onDirectory(dir)) FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return if (onFile(file)) FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
                }

                override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                    logger.error("visit $file failed: $exc")
                    return FileVisitResult.SKIP_SUBTREE
                }
            }
        )
    }

    fun <T> cast(event: WatchEvent<*>?): WatchEvent<T>? {
        @Suppress("UNCHECKED_CAST")
        return event as WatchEvent<T>?
    }
}
