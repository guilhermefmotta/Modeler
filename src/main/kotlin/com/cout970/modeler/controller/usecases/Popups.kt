package com.cout970.modeler.controller.usecases

import com.cout970.modeler.PathConstants
import com.cout970.modeler.api.model.IModel
import com.cout970.modeler.api.model.material.IMaterial
import com.cout970.modeler.controller.tasks.*
import com.cout970.modeler.core.export.ExportProperties
import com.cout970.modeler.core.export.ExportTextureProperties
import com.cout970.modeler.core.export.ImportProperties
import com.cout970.modeler.core.model.AABB
import com.cout970.modeler.core.model.toTRS
import com.cout970.modeler.gui.Gui
import com.cout970.modeler.gui.Popup
import com.cout970.reactive.core.AsyncManager
import com.cout970.vector.extensions.vec2Of
import org.liquidengine.legui.component.Component
import java.io.File

inline fun <reified T> openPopup(gui: Gui, name: String, metadata: Map<String, Any> = emptyMap(),
                                 crossinline func: (T) -> Unit = {}) {

    gui.state.popup = Popup(name, metadata) {
        gui.state.popup = null
        AsyncManager.runLater { gui.root.reRender() }
        (it as? T)?.let { value -> func(value) }
    }
    AsyncManager.runLater { gui.root.reRender() }
}


@UseCase("material.view.config")
private fun showMaterialConfigMenu(gui: Gui, component: Component): ITask = TaskAsync { returnCallback ->
    val old = component.metadata["material"] as IMaterial

    openPopup<IMaterial>(gui, "edit_texture", component.metadata) { new ->
        returnCallback(TaskUpdateMaterial(old, new))
    }
}

@UseCase("project.edit")
private fun showConfigMenu(gui: Gui): ITask = TaskAsync {
    openPopup<Unit>(gui, "config")
}


@UseCase("model.import")
private fun showImportMenu(gui: Gui, model: IModel): ITask = TaskAsync { returnCallback: (ITask) -> Unit ->
    openPopup<ImportProperties>(gui, "import") { prop ->
        returnCallback(TaskImportModel(model, prop))
    }
}

@UseCase("model.export")
private fun showExportMenu(gui: Gui, model: IModel): ITask = TaskAsync { returnCallback: (ITask) -> Unit ->
    openPopup<ExportProperties>(gui, "export") { prop ->
        returnCallback(TaskExportModel(model, prop))
    }
}

@UseCase("model.export.hitboxes")
private fun showExportHitboxMenu(model: IModel): ITask {
    val aabb = model.objectRefs
        .map { model.getObject(it) }
        .filter { it.visible }
        .map { AABB.fromMesh(it.mesh).transform(it.transformation.toTRS()) }

    // TODO fix parent transform not being applied
    return TaskAsync {
        AABB.export(aabb, File(PathConstants.AABB_SAVE_FILE_PATH))
    }
}

@UseCase("texture.export")
private fun showExportTextureMenu(model: IModel, gui: Gui): ITask = TaskAsync { returnCallback: (ITask) -> Unit ->
    openPopup<ExportTextureProperties>(gui, "export_texture") { props ->
        returnCallback(TaskExportTexture(props.path, vec2Of(props.size), model, gui.state.selectedMaterial))
    }
}