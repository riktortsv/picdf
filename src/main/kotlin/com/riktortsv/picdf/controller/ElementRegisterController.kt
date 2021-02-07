package com.riktortsv.picdf.controller

import com.riktortsv.picdf.app.AppProperty
import com.riktortsv.picdf.core.StringUtils
import com.riktortsv.picdf.domain.FileImageElement
import com.riktortsv.picdf.domain.ImageUrlParser
import com.riktortsv.picdf.domain.URLImageElement
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
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import java.net.URL
import java.nio.file.Paths


class ElementRegisterController(val mainWindow: Stage): StackPane() {

    companion object {
        private const val LOCAL_FILE_FOLDER = "LOCAL_FILE_FOLDER"
    }

    @FXML
    private lateinit var tabPane: TabPane

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
    private lateinit var parseWebUrlField: TextField

    @FXML
    private lateinit var parseWebButton: Button

    @FXML
    private lateinit var parseWebUrlsArea: TextArea

    @FXML
    private lateinit var parseWebPageTab: Tab

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

        // 参照ボタンのアクション定義
        localFileBrowseButton.setOnAction { fileChooser() }

        // URL解析のボタン、URLフィールドのアクション定義
        parseWebButton.setOnAction { parseWebPage() }
        parseWebUrlField.setOnAction { parseWebPage() }

        // 表示名
        localFilePathField.textProperty().addListener { o, ov, nv ->
            localFileDisplayField.text = StringUtils.getAsPath(nv)?.fileName?.toString()
        }
        internetFileField.textProperty().addListener { o, ov, nv ->
            internetFileDisplayField.text = StringUtils.getAsUrl(nv)?.toRegex()?.find("[^/]+$")?.value
        }

        // ファイルの一覧をドラッグアンドドロップしたときの動作
        setOnDragOver {
            val db = it.dragboard
            if (it.gestureSource != this && (db.hasFiles() || db.hasString() || db.hasUrl())) {
                it.acceptTransferModes(TransferMode.COPY)
            }
            it.consume()
        }
        setOnDragDropped {
            val db = it.dragboard

            when {
                db.hasFiles() -> {
                    bundledLocalFilesArea.text = db.files.map { it.absolutePath }.joinToString(System.lineSeparator())
                    tabPane.selectionModel.select(bundledLocalFileTab)
                }
                db.hasUrl() -> {
                    var filePaths = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsUrl(it) }.joinToString(System.lineSeparator())
                    if (filePaths.isNotEmpty()) {
                        parseWebUrlsArea.text = filePaths
                        tabPane.selectionModel.select(parseWebPageTab)
                    } else {
                        filePaths = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsPath(it) }.joinToString(System.lineSeparator())
                        bundledLocalFilesArea.text = filePaths
                        tabPane.selectionModel.select(bundledLocalFileTab)
                    }
                }
                db.hasString() -> {
                    var filePaths = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsPath(it) }.joinToString(System.lineSeparator())
                    if (filePaths.isNotEmpty()) {
                        bundledLocalFilesArea.text = filePaths
                        tabPane.selectionModel.select(bundledLocalFileTab)
                    } else {
                        filePaths = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsUrl(it) }.joinToString(System.lineSeparator())
                        parseWebUrlsArea.text = filePaths
                        tabPane.selectionModel.select(parseWebPageTab)
                    }
                }
            }

            it.isDropCompleted = true
            it.consume()
        }
        setOnDragDone {
            it.consume()
        }
    }

    private fun parseWebPage() {
        val url = try {
            URL(parseWebUrlField.text)
        } catch (e: Exception) {
            Alert(Alert.AlertType.INFORMATION, "URLを読み込めません", ButtonType.OK).apply {
                initOwner(mainWindow)
            }.showAndWait()
            return
        }

        viewScope?.launch {
            parseWebButton.isDisable = true
            val urls = withContext(Dispatchers.Default) {
                val parser = ImageUrlParser()
                parser.parse(url).sorted()
            }
            parseWebUrlsArea.text = urls.joinToString("\n")
            parseWebButton.isDisable = false
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
            FileChooser.ExtensionFilter("画像ファイル", "*.png", "*.jpg", "*.gif", "*.bmp"),
            FileChooser.ExtensionFilter("すべてのファイル", "*.*")
        )
        val selectedFile = fileChooser.showOpenDialog(mainWindow)
        if (selectedFile != null) {
            localFilePathField.text = selectedFile.path
            AppProperty.setValue(LOCAL_FILE_FOLDER, selectedFile.absoluteFile.parent)
        }
    }

    private var viewScope: CoroutineScope? = null

    fun showDialog(addTo: ObservableList<ElementViewModel>) {
        viewScope = CoroutineScope(Dispatchers.JavaFx)

        val stage = Stage()
        stage.title = "画像ファイルの追加"
        stage.scene = Scene(this).apply {
            val sets = HashSet(stylesheets)
            sets.addAll(mainWindow.scene.stylesheets)
            stylesheets.setAll(sets)
        }
        stage.isResizable = true
        stage.initOwner(mainWindow)
        stage.initModality(Modality.WINDOW_MODAL)
        stage.showingProperty().addListener { _, ov, nv ->
            if (ov && !nv) {
                addButton.onAction = null
                closeButton.onAction = null
                viewScope?.cancel()
                viewScope = null
            }
        }

        addButton.setOnAction {
            when {
                localFileTab.isSelected -> {
                    StringUtils.getAsPath(localFilePathField.text)?.let {
                        val element = FileImageElement(it)
                        localFileDisplayField.text?.let { element.displayName = it }
                        addTo.add(ElementViewModel(element))
                        localFilePathField.clear()
                    }
                }
                internetFileTab.isSelected -> {
                    StringUtils.getAsUrl(internetFileField.text)?.let {
                        val element = URLImageElement(it)
                        internetFileDisplayField.text?.let { element.displayName = it }
                        addTo.add(ElementViewModel(element))
                        internetFileField.clear()
                    }
                }
                bundledLocalFileTab.isSelected -> {
                    bundledLocalFilesArea.text?.split("\n")?.forEach { path ->
                        StringUtils.getAsPath(path)?.let {
                            val element = FileImageElement(it)
                            addTo.add(ElementViewModel(element))
                        }
                    }
                    bundledLocalFilesArea.clear()
                }
                parseWebPageTab.isSelected -> {
                    parseWebUrlsArea.text?.split("\n")?.forEach { url ->
                        StringUtils.getAsUrl(url)?.let {
                            val element = URLImageElement(it)
                            addTo.add(ElementViewModel(element))
                        }
                    }
                    parseWebUrlsArea.clear()
                }
            }
        }

        closeButton.setOnAction { stage.close() }

        WindowStateSerializer().bind(stage, "REGISTERER")
        stage.showAndWait()
    }
}