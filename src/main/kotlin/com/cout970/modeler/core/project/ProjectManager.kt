package com.cout970.modeler.core.project

import com.cout970.modeler.api.model.IModel
import com.cout970.modeler.core.model.Model
import com.cout970.modeler.core.model.material.IMaterial

/**
 * Created by cout970 on 2017/07/08.
 */

class ProjectManager {

    var project: Project = Project(Author("anonymous"), "unnamed")

    var model: IModel = Model()
        private set

    private val materialList = mutableListOf<IMaterial>()
    val loadedMaterials: List<IMaterial> = materialList

    val modelChangeListeners: MutableList<(old: IModel, new: IModel) -> Unit> = mutableListOf()
    val materialChangeListeners: MutableList<(old: IMaterial?, new: IMaterial?) -> Unit> = mutableListOf()

    fun loadMaterial(material: IMaterial) {
        if (material !in materialList) {
            materialList.add(material)
            materialChangeListeners.forEach { it.invoke(null, material) }
        }
    }

    fun updateMaterial(index: Int, new: IMaterial) {
        if (index in materialList.indices) {
            materialChangeListeners.forEach { it.invoke(materialList[index], new) }
            materialList[index] = new
        }
    }

    fun removeMaterial(material: IMaterial) {
        if (material in loadedMaterials) {
            materialList.remove(material)
            materialChangeListeners.forEach { it.invoke(material, null) }
        }
    }

    fun updateModel(model: IModel) {
        modelChangeListeners.forEach { it.invoke(this.model, model) }
        this.model = model
    }

    fun loadProject(new: Project) {
        project = new
    }
}