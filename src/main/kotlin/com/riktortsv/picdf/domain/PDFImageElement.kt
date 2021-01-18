package com.riktortsv.picdf.domain

import java.awt.image.BufferedImage

interface PDFImageElement {

    val fullPath: String

    var displayName: String

    fun readImage(): BufferedImage

}