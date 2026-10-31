package com.custom.autosplit

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator

class AutoSplitCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return AutoSplitSession()
    }
}

class AutoSplitSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return AutoSplitDashboardScreen(carContext)
    }
}

class AutoSplitDashboardScreen(carContext: CarContext) : Screen(carContext) {

    private var leftMode = "Google Maps"
    private var rightMode = "Media Player"

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // 1. Left Screen Option
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Left Screen: $leftMode")
                .addText("Tap to cycle input (Maps / Waze / Off)")
                .setOnClickListener {
                    leftMode = when (leftMode) {
                        "Google Maps" -> "Waze"
                        "Waze" -> "Off"
                        else -> "Google Maps"
                    }
                    invalidate()
                }
                .build()
        )

        // 2. Right Screen Option
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Right Screen: $rightMode")
                .addText("Tap to cycle input (Media / Web / Off)")
                .setOnClickListener {
                    rightMode = when (rightMode) {
                        "Media Player" -> "Web View"
                        "Web View" -> "Off"
                        else -> "Media Player"
                    }
                    invalidate()
                }
                .build()
        )

        // 3. Launch Action
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Start Split View")
                .addText("Current preset: $leftMode + $rightMode")
                .setOnClickListener {
                    // Logic to start the split view
                }
                .build()
        )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Settings")
                    .setOnClickListener { }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("AutoSplit Dashboard")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .setSingleList(listBuilder.build())
            .build()
    }
}
