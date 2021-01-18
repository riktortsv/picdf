package com.riktortsv.picdf.app

import com.riktortsv.picdf.controller.ViewController
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

fun main() {
    Application.launch(App::class.java)
}

class App : Application() {
    override fun start(primaryStage: Stage) {
        val controller = ViewController(primaryStage)
        primaryStage.scene = Scene(controller)
        primaryStage.show()
    }
}