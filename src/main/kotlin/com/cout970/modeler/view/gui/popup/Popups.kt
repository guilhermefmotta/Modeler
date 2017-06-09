package com.cout970.modeler.view.gui.popup

import com.cout970.modeler.controller.ProjectController
import com.cout970.modeler.core.export.ExportFormat
import com.cout970.modeler.core.export.ExportManager
import com.cout970.modeler.core.log.print
import com.cout970.modeler.core.project.Author
import com.cout970.modeler.core.project.Project
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.swing.JDialog
import javax.swing.JOptionPane

/**
 * Created by cout970 on 2016/12/29.
 */

val popupImage = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB).apply {
    for (i in 0..15) {
        for (j in 0..15) {
            setRGB(i, j, 0xFFFFFF)
        }
    }
}

internal val importFileExtensions: PointerBuffer = MemoryUtil.memAllocPointer(3).apply {
    put(MemoryUtil.memUTF8("*.obj"))
    put(MemoryUtil.memUTF8("*.tcn"))
    put(MemoryUtil.memUTF8("*.json"))
    flip()
}
internal val textureExtensions: PointerBuffer = MemoryUtil.memAllocPointer(1).apply {
    put(MemoryUtil.memUTF8("*.png"))
    flip()
}

private val exportExtensionsObj: PointerBuffer = MemoryUtil.memAllocPointer(1).apply {
    put(MemoryUtil.memUTF8("*.obj"))
    flip()
}
private val exportExtensionsMcx: PointerBuffer = MemoryUtil.memAllocPointer(1).apply {
    put(MemoryUtil.memUTF8("*.mcx"))
    flip()
}

private val saveFileExtension: PointerBuffer = MemoryUtil.memAllocPointer(1).apply {
    put(MemoryUtil.memUTF8("*.pff"))
    flip()
}

private var lastSaveFile: String? = null

fun saveProject(projectManager: ProjectController) {
    if (lastSaveFile == null) {
        saveProjectAs(projectManager)
    } else {
        saveProject(projectManager, projectManager.project)
    }
}

fun saveProjectAs(projectManager: ProjectController) {
    val file = TinyFileDialogs.tinyfd_saveFileDialog("Save As", "", saveFileExtension, "Project File Format (*.pff)")
    if (file != null) {
        lastSaveFile = if (file.endsWith(".pff")) file else file + ".pff"
        saveProject(projectManager, projectManager.project)
    }
}

fun newProject(projectManager: ProjectController, name: String): Boolean {
    if (name.isBlank()) return false
    if (projectManager.project.model.objects.isNotEmpty()) {
        val res = JOptionPane.showConfirmDialog(null,
                "Do you want to create a new project? \nAll unsaved changes will be lost!")
        if (res != JOptionPane.OK_OPTION) return false
    }
    projectManager.newProject(name, Author())
    return true
}

fun loadProject(projectManager: ProjectController, exportManager: ExportManager) {
    if (projectManager.project.model.objects.isNotEmpty()) {
        val res = JOptionPane.showConfirmDialog(null,
                "Do you want to load a new project? \nAll unsaved changes will be lost!")
        if (res != JOptionPane.OK_OPTION) return
    }

    val file = TinyFileDialogs.tinyfd_openFileDialog("Load", "", saveFileExtension, "Project File Format (*.pff)",
            false)
    if (file != null) {
        lastSaveFile = file
        try {
            projectManager.loadProject(exportManager, file)
        } catch (e: Exception) {
            e.print()
        }
    }
}

private fun saveProject(projectManager: ProjectController, project: Project) {
//    projectManager.exportManager.saveProject(lastSaveFile!!, project)
}

fun showImportModelPopup(projectManager: ProjectController) {
    ImportDialog.show { prop ->
        if (prop != null) {
//            projectManager.exportManager.importModel(prop)
        }
    }
}

fun showExportModelPopup(projectManager: ProjectController) {
    ExportDialog.show { prop ->
        if (prop != null) {
//            projectManager.exportManager.exportModel(prop)
        }
    }
}

fun importTexture(projectManager: ProjectController) {
    val file = TinyFileDialogs.tinyfd_openFileDialog("Import Texture", "",
            textureExtensions, "PNG texture (*.png)", false)
    if (file != null) {
//        val sel = projectManager.modelEditor.model.selectAllLeafs()
//        projectManager.exportManager.importTexture(file, sel)
    }
}
//
//fun exportTexture(projectManager: ProjectController) {
//    val file = TinyFileDialogs.tinyfd_saveFileDialog("Export Texture", "texture.png",
//            textureExtensions, "PNG texture (*.png)")
//    if (file != null) {
//        val index = 0
//        val res = projectManager.modelEditor.model.resources
//        val mat: IMaterial
//        val paths: List<ElementPath>
//        if (res.materials.size > index) {
//            mat = res.materials[index]
//            paths = res.pathToMaterial
//                    .entries
//                    .filter { it.value == index }
//                    .map { it.key }
//                    .distinct()
//        } else {
//            mat = MaterialNone
//            paths = projectManager.modelEditor.model.getLeafPaths()
//        }
//        projectManager.exportManager.exportTexture(file, mat, ElementSelection(paths))
//    }
//}

fun Missing(thing: String) {
    JOptionPane.showMessageDialog(null, "Operation not implemented yet: $thing")
}

@Suppress("UNUSED_PARAMETER")
fun getExportFileExtensions(format: ExportFormat): PointerBuffer {
    return when (format) {
        ExportFormat.OBJ -> exportExtensionsObj
        ExportFormat.MCX -> exportExtensionsMcx
    }
}

fun JDialog.center() {
    val toolkit = Toolkit.getDefaultToolkit()
    val x = (toolkit.screenSize.width - width) / 2
    val y = (toolkit.screenSize.height - height) / 2
    location = Point(x, y)
}