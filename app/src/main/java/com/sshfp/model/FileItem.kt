package com.sshfp.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SFTP文件项
 */
data class FileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val permissions: String = "",
    val lastModified: Long = 0,
    val owner: String = "",
    val group: String = "",
    val isLocal: Boolean = false
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> String.format(Locale.getDefault(), "%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun getFormattedDate(): String {
        return if (lastModified > 0) DATE_FORMAT.format(Date(lastModified)) else ""
    }

    fun getExtension(): String {
        val lastDot = name.lastIndexOf('.')
        return if (lastDot > 0) name.substring(lastDot + 1).lowercase() else ""
    }

    fun isTextFile(): Boolean {
        if (isDirectory) return false
        val ext = getExtension()
        val textExtensions = setOf(
            "txt", "md", "json", "xml", "html", "htm", "css", "js", "ts",
            "kt", "java", "py", "c", "cpp", "h", "hpp", "go", "rs",
            "sh", "bash", "zsh", "fish", "ps1", "yml", "yaml", "toml",
            "ini", "cfg", "conf", "log", "csv", "sql", "gradle", "properties"
        )
        return ext in textExtensions
    }
}
