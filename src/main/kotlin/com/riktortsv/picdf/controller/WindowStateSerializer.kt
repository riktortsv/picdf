package com.riktortsv.picdf.controller

import com.riktortsv.picdf.app.AppProperty
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.stage.Stage

class WindowStateSerializer {

    fun bind(stage: Stage, propertyPrefix: String) {
        val propertyX = propertyPrefix + "_POS_X"
        val propertyY = propertyPrefix + "_POS_Y"
        val propertyWidth = propertyPrefix + "_WIDTH"
        val propertyHeight = propertyPrefix + "_HEIGHT"
        val propertyMaximized = propertyPrefix + "_MAXIMIZED"
        val propertyFullScreen = propertyPrefix + "_FULL_SCREEN"

        var previousX = 0.0
        var previousY = 0.0
        var previousWidth = 0.0
        var previousHeight = 0.0

        // 0.1秒遅延させたのち、最大化されていなければ位置・大きさを記憶するサービス
        val delayService = object : Service<Unit>() {
            override fun createTask(): Task<Unit> {
                return object : Task<Unit>() {
                    override fun call() {
                        Thread.sleep(100)
                    }
                }
            }
        }

        delayService.setOnSucceeded {
            if (!stage.isMaximized && !stage.isFullScreen) {
                previousX = stage.x
                previousY = stage.y
                previousWidth = stage.width
                previousHeight = stage.height
            }
        }
        stage.xProperty().addListener { _, _, _ ->
            delayService.restart()
        }
        stage.yProperty().addListener { _, _, _ ->
            delayService.restart()
        }
        stage.widthProperty().addListener { _, _, _ ->
            delayService.restart()
        }
        stage.heightProperty().addListener { _, _, _ ->
            delayService.restart()
        }
        stage.showingProperty().addListener { o, ov, nv ->
            if (ov && !nv) {
                delayService.cancel()
                AppProperty.setValue(propertyX, previousX)
                AppProperty.setValue(propertyY, previousY)
                AppProperty.setValue(propertyWidth, previousWidth)
                AppProperty.setValue(propertyHeight, previousHeight)
                AppProperty.setValue(propertyMaximized, stage.isMaximized)
                AppProperty.setValue(propertyFullScreen, stage.isFullScreen)
            }
        }

        if (AppProperty.containsKey(propertyX)) {
            stage.x = AppProperty.getDouble(propertyX)
        }
        if (AppProperty.containsKey(propertyY)) {
            stage.y = AppProperty.getDouble(propertyY)
        }
        if (AppProperty.containsKey(propertyWidth)) {
            stage.width = AppProperty.getDouble(propertyWidth)
        }
        if (AppProperty.containsKey(propertyHeight)) {
            stage.height = AppProperty.getDouble(propertyHeight)
        }
        if (AppProperty.containsKey(propertyMaximized)) {
            stage.isMaximized = AppProperty.getBoolean(propertyMaximized)
        }
        if (AppProperty.containsKey(propertyFullScreen)) {
            stage.isFullScreen = AppProperty.getBoolean(propertyFullScreen)
        }
    }

}