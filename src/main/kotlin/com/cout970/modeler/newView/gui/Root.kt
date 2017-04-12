package com.cout970.modeler.newView.gui

import com.cout970.modeler.config.Config
import com.cout970.modeler.newView.GuiInitializer
import com.cout970.modeler.newView.gui.comp.CPanel
import com.cout970.modeler.newView.search.ModelView
import com.cout970.modeler.util.*
import com.cout970.vector.extensions.minus
import com.cout970.vector.extensions.plus
import com.cout970.vector.extensions.times
import com.cout970.vector.extensions.vec2Of
import org.joml.Vector2f
import org.liquidengine.legui.component.Frame
import org.liquidengine.legui.util.ColorConstants

/**
 * Created by cout970 on 2017/03/09.
 */
class Root(val initializer: GuiInitializer, val contentPanel: ContentPanel) : Frame(1f, 1f), ITickeable {

    val topBar = TopBar()
    val bottomBar = BottomBar()
    val leftBar = CPanel()
    val rightBar = CPanel()

    val dropdown = CPanel(0f, 20f, 150f, 80f)

    val searchPanel = SearchPanel(ModelView(initializer.buttonController))

    init {
        leftBar.isEnabled = false
        rightBar.isEnabled = false
        topBar.isEnabled = false
        bottomBar.isEnabled = true

        topBar.init(initializer.buttonController, dropdown)
        bottomBar.init(initializer.buttonController, initializer.guiResources)

        listOf(topBar, bottomBar, leftBar, rightBar, contentPanel, dropdown
        ).forEach {
            it.backgroundColor = Config.colorPalette.lightColor.toColor()
        }

        contentPanel.backgroundColor = ColorConstants.transparent()
        dropdown.isVisible = false

        addComponent(topBar)
        addComponent(bottomBar)
        addComponent(leftBar)
        addComponent(rightBar)
        addComponent(contentPanel)
        addComponent(dropdown)
        addComponent(searchPanel)
        addComponent(searchPanel.searchResults)
    }

    override fun tick() {
        initializer.guiResources.updateMaterials(initializer.modelEditor.model)
        initializer.windowHandler.resetViewport()

        contentPanel.updateCamera(initializer.eventController, initializer.windowHandler)
        rescale()
        contentPanel.scaleScenes()
        initializer.viewEventHandler.update()
    }

    fun rescale() {
        size = initializer.windowHandler.window.getFrameBufferSize().toJoml2f()

        topBar.isEnabled = Config.keyBindings.showTopMenu.check(initializer.eventController)

        // Size
        topBar.apply { size = Vector2f(parent.size.x, if (isEnabled) 20f else 0f) }
        bottomBar.apply { size = Vector2f(parent.size.x, if (isEnabled) 32f else 0f) }

        leftBar.apply {
            size = Vector2f(if (isEnabled) 120f else 0f, parent.size.y - topBar.size.y - bottomBar.size.y)
        }
        rightBar.apply {
            size = Vector2f(if (rightBar.isEnabled) 190f else 0f, parent.size.y - topBar.size.y - bottomBar.size.y)
        }

        contentPanel.size = Vector2f(size.x - leftBar.size.x - rightBar.size.x,
                size.y - topBar.size.y - bottomBar.size.y)

        // Position
        topBar.position = Vector2f(0f, 0f)
        bottomBar.position = Vector2f(0f, size.y - bottomBar.size.y)

        leftBar.position = Vector2f(0f, topBar.size.y)
        rightBar.position = Vector2f(leftBar.size.x + contentPanel.size.x, topBar.size.y)

        contentPanel.position = Vector2f(leftBar.size.x, topBar.size.y)

        val center = size.toIVector() * vec2Of(0.5, 0.11)
        searchPanel.apply {
            position = (center - size.toIVector() * 0.5).toJoml2f()
            searchResults.position = Vector2f(position.x, position.y + size.y)
        }

        updateDropdownVisibility()
    }

    private fun updateDropdownVisibility() {
        val mousePos = initializer.eventController.mouse.getMousePos()

        val startTopBar = topBar.position.toIVector()
        val sizeTopBar = topBar.size.toIVector() + vec2Of(0, 20)

        val startDropdown = dropdown.position.toIVector() - vec2Of(20, 0)
        val sizeDropdown = dropdown.size.toIVector() + vec2Of(40, 20)

        if (!mousePos.isInside(startTopBar, sizeTopBar) && !mousePos.isInside(startDropdown, sizeDropdown)) {
            dropdown.isVisible = false
        }
    }
}