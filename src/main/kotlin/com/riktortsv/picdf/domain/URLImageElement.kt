package com.riktortsv.picdf.domain

import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

data class URLImageElement(val url: String): PDFImageElement {

    override val fullPath = url

    override var displayName: String = url.toRegex().find("[^/]+$")?.value ?: ""

    override fun readImage(): BufferedImage {
        val u = URL(url)
//        val con = u.openConnection()
//        con.connectTimeout = 1000 * 60
//        con.readTimeout = 1000 * 60
//        con.connect()
        return ImageIO.read(u)
    }

}
