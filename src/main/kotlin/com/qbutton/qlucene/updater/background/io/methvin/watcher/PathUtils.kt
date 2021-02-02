package com.qbutton.qlucene.updater.background.io.methvin.watcher

import io.methvin.watcher.hashing.FileHash
import io.methvin.watcher.hashing.FileHasher
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.util.SortedMap
import java.util.concurrent.ConcurrentSkipListMap

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

    @Throws(IOException::class)
    fun createHashCodeMap(file: Path, fileHasher: FileHasher?): SortedMap<Path, FileHash> {
        return createHashCodeMap(listOf(file), fileHasher)
    }

    @Throws(IOException::class)
    fun createHashCodeMap(files: List<Path?>, fileHasher: FileHasher?): SortedMap<Path, FileHash> {
        val lastModifiedMap: SortedMap<Path, FileHash> = ConcurrentSkipListMap()
        if (fileHasher != null) {
            for (file in files) {
                for (child in recursiveListFiles(file!!)) {
                    val hash = hash(fileHasher, child)
                    if (hash != null) {
                        lastModifiedMap[child] = hash
                    }
                }
            }
        }
        return lastModifiedMap
    }

    @Throws(IOException::class)
    fun initWatcherState(
        roots: List<Path?>,
        fileHasher: FileHasher?,
        hashes: MutableMap<Path?, FileHash?>,
        directories: MutableSet<Path?>
    ) {
        for (root in roots) {
            if (fileHasher == null) {
                recursiveVisitFiles(root, { e: Path? -> directories.add(e) }) { }
            } else {
                val addHash = { path: Path? ->
                    val hash = hash(fileHasher, path)
                    if (hash != null) hashes[path] = hash
                }
                recursiveVisitFiles(
                    root,
                    { dir: Path? ->
                        directories.add(dir)
                        addHash(dir)
                    },
                    addHash
                )
            }
        }
    }

    @Throws(IOException::class)
    fun recursiveListFiles(file: Path): Set<Path> {
        if (!Files.exists(file)) {
            return emptySet()
        }
        val files: MutableSet<Path> = HashSet()
        files.add(file)
        recursiveVisitFiles(file, { e: Path -> files.add(e) }) { e: Path -> files.add(e) }
        return files
    }

    @Throws(IOException::class)
    fun recursiveVisitFiles(file: Path?, onDirectory: (Path) -> Unit, onFile: (Path) -> Unit) {
        Files.walkFileTree(
            file,
            object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    onDirectory(dir)
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    onFile(file)
                    return FileVisitResult.CONTINUE
                }
            }
        )
    }

    fun <T> cast(event: WatchEvent<*>?): WatchEvent<T>? {
        @Suppress("UNCHECKED_CAST")
        return event as WatchEvent<T>?
    }
}