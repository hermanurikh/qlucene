package com.qbutton.qlucene.dto

/**
 * A class to reflect current state of monitoring or tracking given file.
 * isMonitoredCompletely will be true if it is a directory and it is monitored with all its tree, and false if
 * it is a file, or if only top level is monitored.
 */
data class FileMonitorState(val isDirectory: Boolean, val isMonitoredCompletely: Boolean)
