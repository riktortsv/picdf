package com.riktortsv.picdf.app

import com.riktortsv.picdf.controller.MainPresenter
import com.riktortsv.picdf.core.Injector
import com.riktortsv.picdf.domain.PDFWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files

class PDFWriterService {

    companion object {
        // 縦置きしたときの辺のサイズ
        val A4_VERTICAL_WIDTH: Int = PDFWriter.A4_VERTICAL_WIDTH
        val A4_VERTICAL_HEIGHT: Int = PDFWriter.A4_VERTICAL_HEIGHT
    }

    private val presenter: MainPresenter by lazy { Injector.presenter }

    suspend fun write(params: PDFWriterParams) = withContext(Dispatchers.IO) {
        if (params.elements.isEmpty()) {
            presenter.message("メッセージ", "PDFファイルに書き込む画像ファイルを選択してください")
            presenter.cancel()
            return@withContext
        }

        if (Files.exists(params.savePath)) {
            val isAcceptOverWrite = presenter.confirm("ファイルの上書き", "ファイルが既に存在します。\n上書きしてよろしいですか？")
            // ファイルの上書きを受け入れないときは帰る
            if (!isAcceptOverWrite) {
                presenter.cancel()
                return@withContext
            }
        }

        var count = 0.0
        PDFWriter().use { writer ->
            writer.open(params.savePath, params.width.toFloat(), params.height.toFloat(), params.backgroundColor)
            for (element in params.elements) {
                if (!isActive) {
                    presenter.cancel()
                    return@withContext
                }

                try {
                    writer.acceptImageElement(element)
                    presenter.proceed(element)
                } catch (e: IOException) {
                    presenter.failure(element, "IOエラー")
                } catch (e: NullPointerException) {
                    presenter.failure(element, "画像ファイルではありません")
                } catch (e: Exception) {
                    presenter.error("予期しないエラー", "予期しないエラーが発生しました")
                    presenter.failure(element, e.message ?: "予期しないエラー")
                }

                presenter.progress(++count / params.elements.size)
            }
            presenter.succeed()
        }
    }

}