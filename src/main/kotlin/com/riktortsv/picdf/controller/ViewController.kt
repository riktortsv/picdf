package com.riktortsv.picdf.controller

import com.riktortsv.picdf.app.AppProperty
import com.riktortsv.picdf.app.PDFWriterParams
import com.riktortsv.picdf.app.PDFWriterService
import com.riktortsv.picdf.core.Injector
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.layout.StackPane
import javafx.util.converter.IntegerStringConverter
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.util.*
import kotlin.math.max
import kotlin.math.min


class ViewController(val mainWindow: Stage): StackPane() {

    companion object {
        private const val SAVE_FILE_FOLDER = "SAVE_FILE_FOLDER"
        private const val PDF_WIDTH = "PDF_WIDTH"
        private const val PDF_HEIGHT = "PDF_HEIGHT"
    }

    @FXML
    lateinit var displayNameColumn: TableColumn<ElementViewModel, String>

    @FXML
    lateinit var pathColumn: TableColumn<ElementViewModel, String>

    @FXML
    lateinit var doneColumn: TableColumn<ElementViewModel, Boolean>

    @FXML
    lateinit var resultColumn: TableColumn<ElementViewModel, String>

    @FXML
    lateinit var elementsTable: TableView<ElementViewModel>

    @FXML
    lateinit var upButton: Button

    @FXML
    lateinit var downButton: Button

    @FXML
    lateinit var addButton: Button

    @FXML
    lateinit var removeButton: Button

    @FXML
    lateinit var widthField: TextField

    @FXML
    lateinit var heightField: TextField

    @FXML
    lateinit var pdfSizeMenuButton: MenuButton

    @FXML
    lateinit var clearSizeMenuItem: MenuItem

    @FXML
    lateinit var a4VerticalMenuItem: MenuItem

    @FXML
    lateinit var a4HorizontalMenuItem: MenuItem

    @FXML
    lateinit var savePathField: TextField

    @FXML
    lateinit var browseButton: Button

    @FXML
    lateinit var launchButton: Button

    init {
        val loader = FXMLLoader(javaClass.getResource("/fxml/Main.fxml"))
        loader.setController(this)
        loader.setRoot(this)
        loader.load<Any>()
    }

    private lateinit var controls: Collection<Node>

    fun initialize() {
        // 幅、高さを数字入力のみに
        widthField.textFormatter = TextFormatter(IntegerStringConverter(), 0)
        heightField.textFormatter = TextFormatter(IntegerStringConverter(), 0)
        try {
            if (AppProperty.containsKey(PDF_WIDTH)) {
                widthField.text = AppProperty.getInt(PDF_WIDTH).toString()
            }
            if (AppProperty.containsKey(PDF_HEIGHT)) {
                heightField.text = AppProperty.getInt(PDF_HEIGHT).toString()
            }
        } catch (e: Exception) {
        }

        // テーブル列のフォーマット定義
        displayNameColumn.cellValueFactory = PropertyValueFactory("display")
        pathColumn.cellValueFactory = PropertyValueFactory("path")
        doneColumn.cellValueFactory = PropertyValueFactory("done")
        resultColumn.cellValueFactory = PropertyValueFactory("result")
        displayNameColumn.cellFactory = TextFieldTableCell.forTableColumn()
        pathColumn.cellFactory = TextFieldTableCell.forTableColumn()
        doneColumn.cellFactory = CheckBoxTableCell.forTableColumn(doneColumn)
        resultColumn.cellFactory = TextFieldTableCell.forTableColumn()

        // TableView
        elementsTable.selectionModel.selectionMode = SelectionMode.SINGLE

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
            if (job == null) {
                launch()
            } else {
                cancel()
            }
        }

        controls = listOf(widthField, heightField,
                upButton, downButton,
                addButton, removeButton,
                pdfSizeMenuButton, browseButton
        )

        Injector.presenter = ViewPresenter(this)

        mainWindow.showingProperty().addListener { o, ov, nv ->
            if (!nv) scope.cancel()
        }
    }

    private fun upPriority() {
        val index = elementsTable.selectionModel.selectedIndex
        if (index < 0) return

        Collections.swap(elementsTable.items, index, max(0, index - 1))
    }

    private fun downPriority() {
        val index = elementsTable.selectionModel.selectedIndex
        if (index < 0) return

        Collections.swap(elementsTable.items, index, min(elementsTable.items.size - 1, index + 1))
    }

    private fun addElement() {
        ElementRegisterController(mainWindow).showDialog(elementsTable.items)
    }

    private fun removeElement() {
        val item = elementsTable.selectionModel.selectedItem ?: return
        elementsTable.items.remove(item)
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
    private var job: Job? = null

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
        val items = elementsTable.items.map { it.element }
        val width = widthField.text.toDoubleOrNull() ?: 0.0
        val height = heightField.text.toDoubleOrNull() ?: 0.0

        // プロパティ
        AppProperty.setValue(PDF_WIDTH, width.toInt())
        AppProperty.setValue(PDF_HEIGHT, height.toInt())

        // クリーン
        elementsTable.items.forEach {
            it.resultProperty().value = null
        }

        job = scope.launch(Dispatchers.JavaFx) {
            val jobParam = PDFWriterParams(savePath, width, height, items)
            controls.forEach { it.isDisable = true }
            launchButton.text = "キャンセル"
            service.write(jobParam)
        }
    }

    private fun cancel() {
        job?.cancel()
    }

    fun succeeded() {
        controls.forEach { it.isDisable = false }
        launchButton.text = "開始"
        elementsTable.items.forEach {
            it.doneProperty().value = false
        }
        job = null
    }

    fun canceled() {
        succeeded()
    }

}