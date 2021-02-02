package com.riktortsv.picdf.controller

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import com.riktortsv.picdf.domain.PDFImageElement
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import java.awt.image.BufferedImage

class ElementViewModel(val element: PDFImageElement): RecursiveTreeObject<ElementViewModel>() {

    companion object {
        private object DummyImageElement : PDFImageElement {
            override val fullPath: String = ""
            override var displayName: String = ""
            override fun readImage(): BufferedImage {
                throw NotImplementedError()
            }
        }
        val ROOT = ElementViewModel(DummyImageElement)
    }

    private val displayProperty: StringProperty = SimpleStringProperty(element.displayName)
    fun displayProperty() = displayProperty

    private val pathProperty: StringProperty = SimpleStringProperty(element.fullPath)
    fun pathProperty() = pathProperty

    private val doneProperty: BooleanProperty = SimpleBooleanProperty(false)
    fun doneProperty() = doneProperty

    private val resultProperty: StringProperty = SimpleStringProperty(null)
    fun resultProperty() = resultProperty

}