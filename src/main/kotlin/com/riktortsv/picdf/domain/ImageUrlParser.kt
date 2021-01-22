package com.riktortsv.picdf.domain

import org.jsoup.Jsoup
import java.net.URL

class ImageUrlParser {

    private val extensions = listOf("png", "jpeg", "jpg", "gif", "bmp")
    private fun isAnyMatched(url: String): Boolean {
        return extensions.any(url::endsWith)
    }

    fun parse(parsePage: URL): Set<String> {
        val host = URL(parsePage.protocol, parsePage.host, "")

        val doc = Jsoup.connect(parsePage.toString()).get()
        doc.setBaseUri(host.toString())

        val set = mutableSetOf<String>()

        val img = doc.select("img").map { it.attr("src").toLowerCase() }.filter { isAnyMatched(it) }
        val a = doc.select("a").map { it.attr("href").toLowerCase() }.filter { isAnyMatched(it) }

        set.addAll(img)
        set.addAll(a)

        return set
    }

}