package com.riktortsv.picdf.app

import com.riktortsv.picdf.controller.ViewController
import com.riktortsv.picdf.controller.WindowStateSerializer
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage


fun main() {
    Application.launch(App::class.java)
}

class App : Application() {
    override fun init() {
        super.init()
        AppProperty.load()
    }

    override fun stop() {
        super.stop()
        AppProperty.save()
    }

    override fun start(primaryStage: Stage) {
        WindowStateSerializer().bind(primaryStage, "MAIN")
        val controller = ViewController(primaryStage)
        primaryStage.scene = Scene(controller).apply {
            stylesheets.addAll(
                    App::class.java.getResource("/css/dark.css").toExternalForm(),
                    App::class.java.getResource("/css/app.css").toExternalForm()
            )
        }
        controller.bindKeyCombinations()
        primaryStage.show()
    }
}