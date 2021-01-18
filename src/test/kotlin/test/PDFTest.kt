package test

import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.streams.toList


fun main() {
    try {
        val savePath = Paths.get("F:\\hellohello.pdf")
        Files.deleteIfExists(savePath)

        val width = 800f
        val height = 1000f

        // 空のドキュメントオブジェクトを作成します
        val document = PDDocument(MemoryUsageSetting.setupTempFileOnly())

        // 書き出すサンプル画像集
        val testDir = Paths.get("F:\\images\\開発テスト")
        val files = Files.list(testDir).toList()

        // フォントの生成
        val font: PDFont = PDType1Font.COURIER

        files.forEach { file ->
            // 新しいページのオブジェクトを作成します
            val page = PDPage(PDRectangle(width, height))
            document.addPage(page)
            val img = ImageIO.read(file.toFile())
            val image = LosslessFactory.createFromImage(document, img)

            // 出力用のストリームを開いて画像を描写する
            PDPageContentStream(document, page).use {
                val w = width / image.width
                val h = height / image.height
                val scale = if (abs(w) < abs(h)) w else h

                val writeWidth = image.width * scale
                val writeHeight = image.height * scale

                val pointX = (width - writeWidth) / 2f
                val pointY = (height - writeHeight) / 2f

                it.drawImage(image, pointX, pointY, writeWidth, writeHeight)

                it.beginText()
                it.setFont(font, 12f)
                //it.newLineAtOffset(0f, 0f)
                it.showText("($writeWidth x $writeHeight)")
                it.endText()

                println(file.toAbsolutePath())
            }
        }

        // ドキュメントを保存します
        document.save(savePath.toAbsolutePath().toString())
        document.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}