package com.cout970.modeler.gui

import com.cout970.modeler.util.size
import com.cout970.modeler.util.toJoml2f
import com.cout970.vector.api.IVector2
import org.liquidengine.legui.component.Component
import org.liquidengine.legui.component.Container
import org.liquidengine.legui.component.Frame

/**
 * Created by cout970 on 2017/05/14.
 */

class Root : Frame(1f, 1f) {

    var mainPanel: MutablePanel? = null
        set(value) {
            field = value
            container.clearChilds()
            container.add(value)
        }

    fun updateSizes(newSize: IVector2) {
        size = newSize.toJoml2f()
        mainPanel?.updateSizes(newSize)
    }

    fun loadResources(res: GuiResources) {
        mainPanel?.let { recursiveLoadResources(it, res) }
    }

    private fun recursiveLoadResources(it: Component, res: GuiResources) {
        if (it is IResourceReloadable) it.loadResources(res)
        (it as? Container<*>)?.childs?.forEach { recursiveLoadResources(it, res) }
    }
}