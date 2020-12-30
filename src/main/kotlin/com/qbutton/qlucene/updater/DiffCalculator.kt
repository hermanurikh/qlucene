package com.qbutton.qlucene.updater

import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException

@Component
class DiffCalculator {
    fun getDiff(oldFile: String, newFile: String): String {
        TODO()
    }
}

/*fun main() {
    val f1 = Paths.get("/Users/gurikh/code/qlucene/src/test/resources/files/englishWords1.txt").toFile()
    val f2 = Paths.get("/Users/gurikh/code/qlucene/src/test/resources/files/englishWords2.txt").toFile()

    val lines1 = fileToLines(f1)
    val lines2 = fileToLines(f2)

    val patch = DiffUtils.diff(lines1, lines2)

    println(patch.deltas)
}*/

@Throws(IOException::class)
private fun fileToLines(file: File): List<String>? {
    return file.readLines()
}
