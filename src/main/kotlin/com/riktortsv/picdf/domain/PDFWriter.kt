package com.riktortsv.picdf.domain

import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.awt.Color
import java.io.Closeable
import java.nio.file.Path
import kotlin.math.abs

class PDFWriter: Closeable {

    companion object {
        // 縦置きしたときの辺のサイズ
        val A4_VERTICAL_WIDTH: Int = PDRectangle.A4.width.toInt()
        val A4_VERTICAL_HEIGHT: Int = PDRectangle.A4.height.toInt()
    }

    private var isOpened = false

    private lateinit var savePath: Path
    private var width: Float = 0f
    private var height: Float = 0f
    private lateinit var document: PDDocument
    private lateinit var backgroundColor: Color

    fun open(savePath: Path, width: Float, height: Float, backgroundColor: Color) {
        if (isOpened) throw RuntimeException("already opened")
        this.savePath = savePath
        this.width = width
        this.height = height
        document = PDDocument(MemoryUsageSetting.setupTempFileOnly())
        this.backgroundColor = backgroundColor
        isOpened = true
    }

    fun acceptImageElement(imageElement: PDFImageElement) {
        val img = imageElement.readImage()
        val image = LosslessFactory.createFromImage(document, img)

        if (width < 1 || height < 1) {
            width = image.width.toFloat()
            height = image.height.toFloat()
        }

        val page = PDPage(PDRectangle(width, height))
        document.addPage(page)

        // 出力用のストリームを開いて画像を描画する
        PDPageContentStream(document, page).use {
            // 背景黒
            it.setNonStrokingColor(backgroundColor)
            it.addRect(0f, 0f, width, height)
            it.fill()

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