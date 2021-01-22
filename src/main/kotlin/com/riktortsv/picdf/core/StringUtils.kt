package com.riktortsv.picdf.core

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

object StringUtils {

    fun getAsPath(path: String): Path? {
        return try {
            val p = Paths.get(path)
            return if (Files.isDirectory(p)) null else p
        } catch (e: InvalidPathException) {
            null
        }
    }

    private val urlRegex by lazy { """https?://(?!.+http)[\w/:%#${'$'}&?()~.=+\-]+""".toRegex() }

    fun getAsUrl(url: String): String? {
        return urlRegex.find(url)?.value
    }

}