package com.riktortsv.picdf.controller

import com.riktortsv.picdf.domain.PDFImageElement
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.withContext

class ViewPresenter(val controller: ViewController, val mainWindow: Stage): MainPresenter {

    private fun setAlertStyle(alert: Alert) {
        alert.initOwner(mainWindow)
    }

    private fun getViewModel(element: PDFImageElement): ElementViewModel {
        return controller.elementsTable.items.first { it.element === element }
    }

    override suspend fun proceed(element: PDFImageElement) = withContext(Dispatchers.JavaFx) {
        getViewModel(element).run {
            doneProperty().value = true
            resultProperty().value = "done"
        }
    }

    override suspend fun failure(element: PDFImageElement, cause: String) = withContext(Dispatchers.JavaFx) {
        getViewModel(element).run {
            doneProperty().value = true
            resultProperty().value = cause
        }
    }

    override suspend fun progress(progress: Double) = withContext(Dispatchers.JavaFx) {
        controller.progress.progress = progress
    }

    override suspend fun confirm(title: String, context: String) = withContext(Dispatchers.JavaFx) {
        val alert = Alert(Alert.AlertType.CONFIRMATION, context, ButtonType.YES, ButtonType.NO).apply {
            setAlertStyle(this)
            this.title = title
        }
        val result = alert.showAndWait()
        if (!result.isPresent) return@withContext false
        return@withContext result.get() == ButtonType.YES
    }

    override suspend fun message(title: String, context: String): Unit = withContext(Dispatchers.JavaFx) {
        val alert = Alert(Alert.AlertType.INFORMATION, context, ButtonType.OK).apply {
            setAlertStyle(this)
            this.title = title
        }
        alert.showAndWait()
    }

    override suspend fun error(title: String, context: String): Unit = withContext(Dispatchers.JavaFx) {
        val alert = Alert(Alert.AlertType.ERROR, context, ButtonType.OK).apply {
            setAlertStyle(this)
            this.title = title
        }
        alert.showAndWait()
    }

    override suspend fun succeed() = withContext(Dispatchers.JavaFx) {
        controller.succeeded()
    }

    override suspend fun cancel() = withContext(Dispatchers.JavaFx) {
        controller.canceled()
    }
}