package com.custom.autosplit

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class AutoSplitCarAppService : CarAppService() {
    override fun createHostValidator(): androidx.car.app.validation.HostValidator {
        return androidx.car.app.validation.HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return AutoSplitSession()
    }
}

class AutoSplitSession : Session() {
    private var virtualDisplay: VirtualDisplay? = null
    private var carPresentation: CarPresentation? = null

    override fun onCreateScreen(intent: android.content.Intent): Screen {
        val surfaceCallback = object : SurfaceCallback {
            override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
                val surface = surfaceContainer.surface ?: return
                val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

                virtualDisplay = displayManager.createVirtualDisplay(
                    "AutoSplitDisplay",
                    surfaceContainer.width,
                    surfaceContainer.height,
                    surfaceContainer.dpi,
                    surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                )

                virtualDisplay?.display?.let { disp ->
                    carPresentation = CarPresentation(carContext, disp)
                    carPresentation?.show()
                }
            }

            override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
                carPresentation?.dismiss()
                carPresentation = null
                virtualDisplay?.release()
                virtualDisplay = null
            }
        }

        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
        return DummyScreen(carContext)
    }
}

class DummyScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return PaneTemplate.Builder(
            Pane.Builder()
                .addRow(Row.Builder().setTitle("AutoSplit Active").build())
                .build()
        ).setHeaderAction(Action.APP_ICON).build()
    }
}
