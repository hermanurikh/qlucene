package com.qbutton.qlucene.integration

const val resourcesRoot = "src/test/resources"
const val level1DirName = "level1dir_1"
const val level1Dir = "$resourcesRoot/$level1DirName"
const val level2DirName = "level2dir_1"
const val level2Dir = "$level1Dir/$level2DirName"
const val nestedFileName = "simpleFile2.txt"
const val nestedFile = "$level2Dir/$nestedFileName"

const val tmpDir = "tmp-test"
const val tmpTestDir = "$tmpDir/test"
const val tmpTestNestedDir = "$tmpTestDir/level1"
