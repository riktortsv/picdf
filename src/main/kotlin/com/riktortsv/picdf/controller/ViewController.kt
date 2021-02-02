package com.riktortsv.picdf.controller

import com.jfoenix.controls.*
import com.riktortsv.picdf.app.AppProperty
import com.riktortsv.picdf.app.PDFWriterParams
import com.riktortsv.picdf.app.PDFWriterService
import com.riktortsv.picdf.core.Injector
import com.riktortsv.picdf.core.StringUtils
import com.riktortsv.picdf.domain.FileImageElement
import com.riktortsv.picdf.domain.URLImageElement
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.CheckBoxTreeTableCell
import javafx.scene.input.TransferMode
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.converter.IntegerStringConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import java.awt.Color
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.util.*
import kotlin.math.max
import kotlin.math.min


class ViewController(val mainWindow: Stage): StackPane() {

    companion object {
        private const val SAVE_FILE_FOLDER = "SAVE_FILE_FOLDER"
        private const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
        private const val PDF_WIDTH = "PDF_WIDTH"
        private const val PDF_HEIGHT = "PDF_HEIGHT"
    }

    @FXML
    lateinit var displayNameColumn: JFXTreeTableColumn<ElementViewModel, String>

    @FXML
    lateinit var pathColumn: JFXTreeTableColumn<ElementViewModel, String>

    @FXML
    lateinit var doneColumn: JFXTreeTableColumn<ElementViewModel, Boolean>

    @FXML
    lateinit var resultColumn: JFXTreeTableColumn<ElementViewModel, String>

    @FXML
    lateinit var elementsTable: JFXTreeTableView<ElementViewModel>

    @FXML
    lateinit var upButton: JFXButton

    @FXML
    lateinit var downButton: JFXButton

    @FXML
    lateinit var addButton: JFXButton

    @FXML
    lateinit var removeButton: JFXButton

    @FXML
    lateinit var colorPicker: JFXColorPicker

    @FXML
    lateinit var widthField: JFXTextField

    @FXML
    lateinit var heightField: JFXTextField

    @FXML
    lateinit var pdfSizeMenuButton: JFXNodesList

    @FXML
    lateinit var clearSizeMenuItem: JFXButton

    @FXML
    lateinit var a4VerticalMenuItem: JFXButton

    @FXML
    lateinit var a4HorizontalMenuItem: JFXButton

    @FXML
    lateinit var savePathField: JFXTextField

    @FXML
    lateinit var browseButton: JFXButton

    @FXML
    lateinit var launchButton: JFXButton

    @FXML
    lateinit var progress: JFXSpinner

    init {
        val loader = FXMLLoader(javaClass.getResource("/fxml/Main.fxml"))
        loader.setController(this)
        loader.setRoot(this)
        loader.load<Any>()
    }

    private lateinit var controls: Collection<Node>

    lateinit var elementItems: ObservableList<ElementViewModel>

    private lateinit var tableRootItem: RecursiveTreeItem<ElementViewModel>

    fun initialize() {
        // 幅、高さを数字入力のみに
        widthField.textFormatter = TextFormatter(IntegerStringConverter(), 0)
        heightField.textFormatter = TextFormatter(IntegerStringConverter(), 0)
        // 前回実行時のプロパティを復元
        try {
            if (AppProperty.containsKey(PDF_WIDTH)) {
                widthField.text = AppProperty.getInt(PDF_WIDTH).toString()
            }
            if (AppProperty.containsKey(PDF_HEIGHT)) {
                heightField.text = AppProperty.getInt(PDF_HEIGHT).toString()
            }
        } catch (e: Exception) {
        }
        try {
            if (AppProperty.containsKey(BACKGROUND_COLOR)) {
                val rgb = AppProperty.getInt(BACKGROUND_COLOR)
                val r: Int = rgb.shr(16).and(0xff)
                val g: Int = rgb.shr(8).and(0xff)
                val b: Int = rgb.shr(0).and(0xff)
                colorPicker.value = javafx.scene.paint.Color.rgb(r, g, b)
            }
        } catch (e: Exception) {
        }

        elementItems = FXCollections.observableArrayList()
        tableRootItem = RecursiveTreeItem(elementItems) { it.children }

        // テーブル列のフォーマット定義
        fun <T> setupCellValueFactory(
            column: JFXTreeTableColumn<ElementViewModel, T>,
            mapper: (ElementViewModel?) -> ObservableValue<T>
        ) {
            column.setCellValueFactory { param: TreeTableColumn.CellDataFeatures<ElementViewModel, T> ->
                if (column.validateValue(param)) {
                    if (param.value == null) {
                        return@setCellValueFactory mapper(null)
                    } else {
                        return@setCellValueFactory mapper(param.value.value)
                    }
                } else {
                    return@setCellValueFactory column.getComputedValue(param)
                }
            }
        }
        setupCellValueFactory(displayNameColumn) { it?.displayProperty() ?: SimpleStringProperty() }
        setupCellValueFactory(pathColumn) { it?.pathProperty() ?: SimpleStringProperty() }
        setupCellValueFactory(doneColumn) { it?.doneProperty() ?: SimpleBooleanProperty() }
        setupCellValueFactory(resultColumn) { it?.resultProperty() ?: SimpleStringProperty() }
        doneColumn.cellFactory = CheckBoxTreeTableCell.forTreeTableColumn(doneColumn)

        // TableView
        elementsTable.selectionModel.selectionMode = SelectionMode.SINGLE
        elementsTable.root = tableRootItem
        elementsTable.isShowRoot = false

        // 一覧操作ボタン
        upButton.setOnAction { upPriority() }
        downButton.setOnAction { downPriority() }
        addButton.setOnAction { addElement() }
        removeButton.setOnAction { removeElement() }

        // 出力サイズ
        clearSizeMenuItem.setOnAction { clearPDFSize() }
        a4VerticalMenuItem.setOnAction { setPDFSizeA4Vertical() }
        a4HorizontalMenuItem.setOnAction { setPDFSizeA4Horizontal() }

        // 機能ボタン
        browseButton.setOnAction { browseSavePath() }
        launchButton.setOnAction {
            if (task == null) {
                launch()
            } else {
                cancel()
            }
        }

        // ファイルの一覧をドラッグアンドドロップしたときの動作
        setOnDragOver { e ->
            val db = e.dragboard
            if (e.gestureSource != this && (db.hasFiles() || db.hasString() || db.hasUrl())) {
                e.acceptTransferModes(TransferMode.COPY)
            }
            e.consume()
        }
        setOnDragDropped { e ->
            val db = e.dragboard

            // クリップボードの内容に応じて分岐してImageElementに変換
            val elements = when {
                db.hasFiles() -> {
                    db.files.map { it.toPath() }.map { FileImageElement(it) }
                }
                db.hasUrl() -> {
                    val filePaths = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsUrl(it) }
                    if (filePaths.isNotEmpty()) {
                        filePaths.map { URLImageElement(it) }
                    } else {
                        val paths = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsPath(it) }
                        paths.map { FileImageElement(it) }
                    }
                }
                db.hasString() -> {
                    val filePaths = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsPath(it) }
                    if (filePaths.isNotEmpty()) {
                        filePaths.map { FileImageElement(it) }
                    } else {
                        val urls = db.string.split(System.lineSeparator()).mapNotNull { StringUtils.getAsUrl(it) }
                        urls.map { URLImageElement(it) }
                    }
                }
                else -> emptyList()
            }.map { ElementViewModel(it) }

            // テーブルに追加
            elementItems.addAll(elements)

            e.isDropCompleted = true
            e.consume()
        }
        setOnDragDone { e ->
            e.consume()
        }

        controls = listOf(colorPicker, widthField, heightField,
                upButton, downButton,
                addButton, removeButton,
                pdfSizeMenuButton, browseButton
        )

        Injector.presenter = ViewPresenter(this)

        mainWindow.showingProperty().addListener { o, ov, nv ->
            if (!nv) scope.cancel()
        }

        progress.visibleProperty().bind(progress.indeterminateProperty().not())

        stylesheets.add(javaClass.getResource("/css/jfoenix.css").toExternalForm())
    }

    private fun upPriority() {
        val index = elementsTable.selectionModel.selectedIndex
        if (index < 0) return

        Collections.swap(elementItems, index, max(0, index - 1))
    }

    private fun downPriority() {
        val index = elementsTable.selectionModel.selectedIndex
        if (index < 0) return

        Collections.swap(elementItems, index, min(elementItems.size - 1, index + 1))
    }

    private fun addElement() {
        ElementRegisterController(mainWindow).showDialog(elementItems)
    }

    private fun removeElement() {
        val item = elementsTable.selectionModel.selectedItem?.value ?: return
        elementItems.remove(item)
    }

    private fun browseSavePath() {
        val fileChooser = FileChooser()
        if (AppProperty.containsKey(SAVE_FILE_FOLDER)) {
            try {
                fileChooser.initialDirectory = Paths.get(AppProperty.getString(SAVE_FILE_FOLDER)).toFile()
            } catch (e: Exception) {
            }
        }
        fileChooser.title = "ファイルの保存"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("PDFファイル", "*.pdf")
        )
        val selectedFile = fileChooser.showSaveDialog(mainWindow)
        if (selectedFile != null) {
            savePathField.text = selectedFile.path
            AppProperty.setValue(SAVE_FILE_FOLDER, selectedFile.absoluteFile.parent)
        }
    }

    private fun clearPDFSize() {
        widthField.clear()
        heightField.clear()
    }

    private fun setPDFSizeA4Vertical() {
        widthField.text = PDFWriterService.A4_VERTICAL_WIDTH.toString()
        heightField.text = PDFWriterService.A4_VERTICAL_HEIGHT.toString()
    }

    private fun setPDFSizeA4Horizontal() {
        widthField.text = PDFWriterService.A4_VERTICAL_HEIGHT.toString()
        heightField.text = PDFWriterService.A4_VERTICAL_WIDTH.toString()
    }

    private val scope by lazy { CoroutineScope(Dispatchers.JavaFx) }
    private var task: Job? = null

    private val service by lazy { PDFWriterService() }

    private fun launch() {
        if (savePathField.text?.length ?: 0 < 1) {
            savePathField.requestFocus()
            return
        }
        val savePath = try {
            Paths.get(savePathField.text)
        } catch (e: InvalidPathException) {
            savePathField.requestFocus()
            return
        }
        val items = elementItems.map { it.element }
        val width = widthField.text.toDoubleOrNull() ?: 0.0
        val height = heightField.text.toDoubleOrNull() ?: 0.0

        val pickedColor = colorPicker.value
        val color = Color(pickedColor.red.toFloat(), pickedColor.green.toFloat(), pickedColor.blue.toFloat())

        // プロパティ
        AppProperty.setValue(BACKGROUND_COLOR, color.rgb)
        AppProperty.setValue(PDF_WIDTH, width.toInt())
        AppProperty.setValue(PDF_HEIGHT, height.toInt())

        // クリーン
        elementItems.forEach {
            it.resultProperty().value = null
        }

        task = scope.launch(Dispatchers.JavaFx) {
            try {
                // タスクパラメータ
                val taskParams = PDFWriterParams(savePath, width, height, color, items)

                // UI変更
                controls.forEach { it.isDisable = true }
                launchButton.text = "キャンセル"
                elementsTable.columns.forEach { it.isSortable = false }

                // タスク実行
                service.write(taskParams)
            } catch (e: CancellationException) {
                canceled()
            }
        }
    }

    private fun cancel() {
        task?.cancel()
    }

    fun succeeded() {
        try {
            // UI変更
            controls.forEach { it.isDisable = false }
            launchButton.text = "開始"
            elementItems.forEach {
                it.doneProperty().value = false
            }
            elementsTable.columns.forEach { it.isSortable = false }

            // タスククリア
            task = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun canceled() {
        succeeded()
    }

}