package com.custom.autosplit

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
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

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Start Split View")
                .addText("Current preset: $leftMode + $rightMode")
                .setOnClickListener {
                    CarToast.makeText(
                        carContext,
                        "Starting Split Display...",
                        CarToast.LENGTH_SHORT
                    ).show()

                    screenManager.push(SplitPresentationScreen(carContext))
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

class SplitPresentationScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarPresentation? = null

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
    }

    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Exit Split")
                            .setOnClickListener {
                                screenManager.pop()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        val width = surfaceContainer.width.takeIf { it > 0 } ?: 1280
        val height = surfaceContainer.height.takeIf { it > 0 } ?: 720
        val dpi = surfaceContainer.dpi.takeIf { it > 0 } ?: 160

        try {
            val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            virtualDisplay = displayManager.createVirtualDisplay(
                "AutoSplitVirtualDisplay",
                width,
                height,
                dpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )

            virtualDisplay?.display?.let { display ->
                presentation = CarPresentation(carContext, display)
                presentation?.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        presentation?.dismiss()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }
}
