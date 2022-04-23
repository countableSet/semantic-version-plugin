package dev.poolside.gradle.semanticversion

import java.io.File

object FileManager {
    fun writeFile(folder: File, path: String = "", filename: String, content: String) {
        val fullPath = "${folder.absolutePath}/${path}"
        val f = File(fullPath)
        f.mkdirs()
        File(f, filename).writeText(content)
    }
}