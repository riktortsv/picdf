package com.riktortsv.picdf.controller

import com.riktortsv.picdf.domain.FileImageElement
import com.riktortsv.picdf.domain.PDFImageElement
import com.riktortsv.picdf.domain.URLImageElement
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.*
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import javafx.scene.input.TransferMode

import javafx.scene.input.DragEvent





class ElementRegisterController(val mainWindow: Stage): StackPane() {

    @FXML
    private lateinit var localFileTab: Tab

    @FXML
    private lateinit var localFilePathField: TextField

    @FXML
    private lateinit var localFileBrowseButton: Button

    @FXML
    private lateinit var localFileDisplayField: TextField

    @FXML
    private lateinit var internetFileTab: Tab

    @FXML
    private lateinit var internetFileField: TextField

    @FXML
    private lateinit var internetFileDisplayField: TextField

    @FXML
    private lateinit var bundledLocalFileTab: Tab

    @FXML
    private lateinit var bundledLocalFilesArea: TextArea

    @FXML
    private lateinit var bundledLocalInternetTab: Tab

    @FXML
    private lateinit var bundledInternetFilesArea: TextArea

    init {
        val loader = FXMLLoader(javaClass.getResource("/fxml/ElementRegister.fxml"))
        loader.setController(this)
        loader.setRoot(this)
        loader.load<Any>()
    }

    fun initialize() {
        // プロンプトメッセージを手動で設定
        bundledLocalFilesArea.promptText = "ex.)\r\n" +
                "D:\\image1.jpg\r\n" +
                "D:\\image2.jpg\r\n" +
                "D:\\photo.png"
        bundledInternetFilesArea.promptText = "ex.)\r\n" +
                "https://picdf.com/image1.jpg\r\n" +
                "https://picdf.com/image2.jpg\r\n" +
                "https://picdf.com/photo.gif"

        // 参照ボタンのアクション定義
        localFileBrowseButton.setOnAction { fileChooser() }

        bundledLocalFilesArea.setOnDragOver {
            if (it.gestureSource != bundledLocalFilesArea && it.dragboard.hasFiles()) {
                it.acceptTransferModes(TransferMode.COPY)
            }
            it.consume()
        }
        bundledLocalFilesArea.setOnDragDropped {
            val db = it.dragboard

            bundledLocalFilesArea.text = db.files.map { it.absolutePath }.joinToString("\n")

            it.isDropCompleted = true
            it.consume()
        }
        bundledLocalFilesArea.setOnDragDone {
            it.consume()
        }
    }

    private fun fileChooser() {
        val fileChooser = FileChooser()
        fileChooser.title = "画像ファイルの選択"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("画像ファイル", "*.png", "*.jpg", "*.gif"),
            FileChooser.ExtensionFilter("すべてのファイル", "*.*")
        )
        val selectedFile = fileChooser.showOpenDialog(mainWindow)
        if (selectedFile != null) {
            localFilePathField.text = selectedFile.path
        }
    }

    private fun getAsPath(path: String): Path? {
        return try {
            Paths.get(path)
        } catch (e: InvalidPathException) {
            null
        }
    }

    private val urlRegex by lazy { """https?://(?!.+http)[\w/:%#${'$'}&?()~.=+\-]+""".toRegex() }

    private fun getAsUrl(url: String): String? {
        return urlRegex.find(url)?.value
    }

    fun showDialog(): ObservableList<PDFImageElement> {
        val dialog = Dialog<ButtonType>()
        dialog.title = "画像ファイルの選択"
        dialog.dialogPane.content = this
        dialog.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        val result = FXCollections.observableArrayList<PDFImageElement>()
        dialog.showAndWait().ifPresent {
            if (it != ButtonType.OK) return@ifPresent

            when {
                localFileTab.isSelected -> {
                    getAsPath(localFilePathField.text)?.let {
                        val element = FileImageElement(it)
                        localFileDisplayField.text?.let { element.displayName = it }
                        result.add(element)
                    }
                }
                internetFileTab.isSelected -> {
                    getAsUrl(internetFileField.text)?.let {
                        val element = URLImageElement(it)
                        internetFileDisplayField.text?.let { element.displayName = it }
                        result.add(element)
                    }
                }
                bundledLocalFileTab.isSelected -> {
                    bundledLocalFilesArea.text?.split("\n")?.forEach { path ->
                        getAsPath(path)?.let {
                            val element = FileImageElement(it)
                            result.add(element)
                        }
                    }
                }
                bundledLocalInternetTab.isSelected -> {
                    bundledLocalFilesArea.text?.split("\n")?.forEach { url ->
                        getAsUrl(url)?.let {
                            val element = URLImageElement(it)
                            result.add(element)
                        }
                    }
                }
            }
        }

        return result
    }
}