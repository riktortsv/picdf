package com.riktortsv.picdf.controller

import com.riktortsv.picdf.app.AppProperty
import com.riktortsv.picdf.domain.FileImageElement
import com.riktortsv.picdf.domain.PDFImageElement
import com.riktortsv.picdf.domain.URLImageElement
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.TransferMode
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths


class ElementRegisterController(val mainWindow: Stage): StackPane() {

    companion object {
        private const val LOCAL_FILE_FOLDER = "LOCAL_FILE_FOLDER"
    }

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

    @FXML
    private lateinit var addButton: Button

    @FXML
    private lateinit var closeButton: Button

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

        // 表示名
        localFilePathField.textProperty().addListener { o, ov, nv ->
            localFileDisplayField.text = getAsPath(nv)?.fileName?.toString()
        }
        internetFileField.textProperty().addListener { o, ov, nv ->
            internetFileDisplayField.text = getAsUrl(nv)?.toRegex()?.find("[^/]+$")?.value
        }

        // ファイルの一覧をドラッグアンドドロップしたときの動作
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
        if (AppProperty.containsKey(LOCAL_FILE_FOLDER)) {
            try {
                fileChooser.initialDirectory = Paths.get(AppProperty.getString(LOCAL_FILE_FOLDER)).toFile()
            } catch (e: Exception) {
            }
        }
        fileChooser.title = "画像ファイルの選択"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("画像ファイル", "*.png", "*.jpg", "*.gif"),
            FileChooser.ExtensionFilter("すべてのファイル", "*.*")
        )
        val selectedFile = fileChooser.showOpenDialog(mainWindow)
        if (selectedFile != null) {
            localFilePathField.text = selectedFile.path
            AppProperty.setValue(LOCAL_FILE_FOLDER, selectedFile.absoluteFile.parent)
        }
    }

    private fun getAsPath(path: String): Path? {
        return try {
            val p = Paths.get(path)
            if (Files.isDirectory(p)) return null else return p
        } catch (e: InvalidPathException) {
            null
        }
    }

    private val urlRegex by lazy { """https?://(?!.+http)[\w/:%#${'$'}&?()~.=+\-]+""".toRegex() }

    private fun getAsUrl(url: String): String? {
        return urlRegex.find(url)?.value
    }

    fun showDialog(addTo: ObservableList<ElementViewModel>) {
        val stage = Stage()
        stage.title = "画像ファイルの追加"
        stage.scene = Scene(this)
        stage.isResizable = false
        stage.initOwner(mainWindow)
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.showingProperty().addListener { _, ov, nv ->
            if (ov && !nv) {
                addButton.onAction = null
                closeButton.onAction = null
            }
        }

        addButton.setOnAction {
            when {
                localFileTab.isSelected -> {
                    getAsPath(localFilePathField.text)?.let {
                        val element = FileImageElement(it)
                        localFileDisplayField.text?.let { element.displayName = it }
                        addTo.add(ElementViewModel(element))
                        localFilePathField.clear()
                    }
                }
                internetFileTab.isSelected -> {
                    getAsUrl(internetFileField.text)?.let {
                        val element = URLImageElement(it)
                        internetFileDisplayField.text?.let { element.displayName = it }
                        addTo.add(ElementViewModel(element))
                        internetFileField.clear()
                    }
                }
                bundledLocalFileTab.isSelected -> {
                    bundledLocalFilesArea.text?.split("\n")?.forEach { path ->
                        getAsPath(path)?.let {
                            val element = FileImageElement(it)
                            addTo.add(ElementViewModel(element))
                        }
                    }
                    bundledLocalFilesArea.clear()
                }
                bundledLocalInternetTab.isSelected -> {
                    bundledLocalFilesArea.text?.split("\n")?.forEach { url ->
                        getAsUrl(url)?.let {
                            val element = URLImageElement(it)
                            addTo.add(ElementViewModel(element))
                        }
                    }
                    bundledLocalFilesArea.clear()
                }
            }
        }

        closeButton.setOnAction { stage.close() }
        stage.showAndWait()
    }
}