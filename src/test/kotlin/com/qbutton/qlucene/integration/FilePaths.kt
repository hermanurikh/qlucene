package com.qbutton.qlucene.integration

import java.io.File

const val rootDirName = "rootdir"
const val rootDir2Name = "rootdir2"
const val rootDir3Name = "rootdir3"
const val rootDir4Name = "rootdir4"
const val nestedDirName = "nesteddir"
const val nestedFileName = "simpleFile2.txt"
const val tmpDir = "tmp-test"

val fileSeparator = File.separator!!
val resourcesRoot = "src${fileSeparator}test${fileSeparator}resources${fileSeparator}testfiles"
val rootDir = "$resourcesRoot${fileSeparator}$rootDirName"
val nestedDir = "$rootDir${fileSeparator}$nestedDirName"
val nestedFile = "$nestedDir${fileSeparator}$nestedFileName"
val rootDir2 = "$resourcesRoot${fileSeparator}$rootDir2Name"
val rootDir3 = "$resourcesRoot${fileSeparator}$rootDir3Name"

val rootDir4 = "$resourcesRoot${fileSeparator}$rootDir4Name"
val tmpTestDir = "$tmpDir${fileSeparator}test"
val tmpTestNestedDir = "$tmpTestDir${fileSeparator}level1"
