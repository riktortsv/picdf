package com.riktortsv.picdf.domain

import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO

data class FileImageElement(private val path: Path): PDFImageElement {

    override val fullPath = path.toAbsolutePath().toString()

    override var displayName = path.fileName.toString()

    override fun readImage(): BufferedImage {
        return ImageIO.read(path.toFile())
    }

}