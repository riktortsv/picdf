package com.riktortsv.picdf.controller

import com.riktortsv.picdf.domain.PDFImageElement
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class ElementViewModel(val element: PDFImageElement) {

    private val displayProperty: StringProperty = SimpleStringProperty(element.displayName)
    fun displayProperty() = displayProperty

    private val pathProperty: StringProperty = SimpleStringProperty(element.fullPath)
    fun pathProperty() = pathProperty

    private val doneProperty: BooleanProperty = SimpleBooleanProperty(false)
    fun doneProperty() = doneProperty

    private val resultProperty: StringProperty = SimpleStringProperty(null)
    fun resultProperty() = resultProperty

}