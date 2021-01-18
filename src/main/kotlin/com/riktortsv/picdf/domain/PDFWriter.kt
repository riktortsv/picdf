package com.riktortsv.picdf.domain

import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.Closeable
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.abs

class PDFWriter: Closeable {

    private var isOpened = false

    private lateinit var savePath: Path
    private var width: Float = 0f
    private var height: Float = 0f
    private lateinit var document: PDDocument

    fun open(savePath: Path, width: Float, height: Float) {
        if (isOpened) throw RuntimeException("already opened")
        this.savePath = savePath
        this.width = width
        this.height = height
        document = PDDocument(MemoryUsageSetting.setupTempFileOnly())
        isOpened = true
    }

    fun acceptImageElement(imageElement: PDFImageElement) {
        val img = imageElement.readImage()
        val image = LosslessFactory.createFromImage(document, img)

        if (width < 1 || height < 1) {
//            width = image.width.toFloat()
//            height = image.height.toFloat()
            width = PDRectangle.A4.width
            height = PDRectangle.A4.height
        }

        val page = PDPage(PDRectangle(width, height))
        document.addPage(page)

        // 出力用のストリームを開いて画像を描画する
        PDPageContentStream(document, page).use {
            val w = width / image.width
            val h = height / image.height
            val scale = if (abs(w) < abs(h)) w else h

            val writeWidth = image.width * scale
            val writeHeight = image.height * scale

            val pointX = (width - writeWidth) / 2f
            val pointY = (height - writeHeight) / 2f

            it.drawImage(image, pointX, pointY, writeWidth, writeHeight)
        }
    }

    fun write() {
        close()
    }

    override fun close() {
        if (!isOpened) return
        document.save(savePath.toAbsolutePath().toString())
        document.close()
        isOpened = false
    }

}