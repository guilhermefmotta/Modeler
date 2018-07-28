package com.cout970.modeler.core.helpers

import com.cout970.matrix.api.IMatrix4
import com.cout970.modeler.api.model.IModel
import com.cout970.modeler.api.model.ITransformation
import com.cout970.modeler.api.model.`object`.IObject
import com.cout970.modeler.api.model.`object`.IObjectCube
import com.cout970.modeler.api.model.mesh.IMesh
import com.cout970.modeler.api.model.selection.ISelection
import com.cout970.modeler.api.model.selection.SelectionType
import com.cout970.modeler.api.model.selection.toObjectRef
import com.cout970.modeler.core.model.*
import com.cout970.modeler.core.model.mesh.FaceIndex
import com.cout970.modeler.core.model.mesh.Mesh
import com.cout970.modeler.core.model.mesh.getTextureVertex
import com.cout970.modeler.render.tool.Animator
import com.cout970.modeler.util.*
import com.cout970.vector.api.IVector2
import com.cout970.vector.api.IVector3
import com.cout970.vector.extensions.*
import org.joml.Matrix4d
import org.joml.Vector4d

/**
 * Created by cout970 on 2017/02/11.
 */
object TransformationHelper {

    fun transformLocal(source: IModel, sel: ISelection, animator: Animator, transform: ITransformation): IModel {
        return when (sel.selectionType) {
            SelectionType.OBJECT -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    obj.withTransformation(obj.transformation + (inv + transform))
                }
            }
            SelectionType.FACE -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    val indices: Set<Int> = sel.faces
                            .filter { it.objectId == obj.id }
                            .map { obj.mesh.faces[it.faceIndex] }
                            .flatMap { it.pos }
                            .toSet()

                    val local = obj.transformation + transform + obj.transformation.invert()
                    val newMesh = transformMesh(obj, indices, (inv + local).matrix)
                    obj.withMesh(newMesh)
                }
            }
            SelectionType.EDGE -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    val indices: Set<Int> = sel.edges
                            .filter { it.objectId == obj.id }
                            .flatMap { listOf(it.firstIndex, it.secondIndex) }
                            .toSet()

                    val local = obj.transformation + transform + obj.transformation.invert()
                    val newMesh = transformMesh(obj, indices, (inv + local).matrix)
                    obj.withMesh(newMesh)
                }
            }
            SelectionType.VERTEX -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    val indices: Set<Int> = sel.pos
                            .filter { it.objectId == obj.id }
                            .map { it.posIndex }
                            .toSet()

                    val local = obj.transformation + transform + obj.transformation.invert()
                    val newMesh = transformMesh(obj, indices, (inv + local).matrix)
                    obj.withMesh(newMesh)
                }
            }
        }
    }

    fun scaleLocal(source: IModel, sel: ISelection, animator: Animator, vector: IVector3, offset: Float): IModel {

        fun calculateTransform(obj: IObject, inv: ITransformation): ITransformation {
            val trs = obj.transformation.toTRS()
            val local = trs.rotation.invert().transform(inv.matrix.transformVertex(vector))
            val (scale, translation) = getScaleAndTranslation(local)
            val finalTranslation = trs.rotation.transform(inv.invert().matrix.transformVertex(translation))

            return trs.copy(
                    translation = trs.translation + finalTranslation * offset,
                    scale = (trs.scale + scale * offset).max(Vector3.ZERO)
            )
        }

        return when (sel.selectionType) {
            SelectionType.OBJECT -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    obj.withTransformation(calculateTransform(obj, inv))
                }
            }
            SelectionType.FACE -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    val indices: Set<Int> = sel.faces
                            .filter { it.objectId == obj.id }
                            .map { obj.mesh.faces[it.faceIndex] }
                            .flatMap { it.pos }
                            .toSet()

                    val trs = obj.transformation.toTRS()
                    val transform = calculateTransform(obj, inv) + trs.invert()

                    val newMesh = transformMesh(obj, indices, (inv + transform).matrix)
                    obj.withMesh(newMesh)
                }
            }
            SelectionType.EDGE -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    val indices: Set<Int> = sel.edges
                            .filter { it.objectId == obj.id }
                            .flatMap { listOf(it.firstIndex, it.secondIndex) }
                            .toSet()

                    val trs = obj.transformation.toTRS()
                    val transform = calculateTransform(obj, inv) + trs.invert()

                    val newMesh = transformMesh(obj, indices, (inv + transform).matrix)
                    obj.withMesh(newMesh)
                }
            }
            SelectionType.VERTEX -> {
                applyTransformation(source, sel, animator) { obj, inv ->
                    val indices: Set<Int> = sel.pos
                            .filter { it.objectId == obj.id }
                            .map { it.posIndex }
                            .toSet()

                    val trs = obj.transformation.toTRS()
                    val transform = calculateTransform(obj, inv) + trs.invert()

                    val newMesh = transformMesh(obj, indices, (inv + transform).matrix)
                    obj.withMesh(newMesh)
                }
            }
        }
    }

    private fun getScaleAndTranslation(vec: IVector3): Pair<IVector3, IVector3> {
        val x = vec.xd >= 0
        val y = vec.yd >= 0
        val z = vec.zd >= 0

        return when {
            !x && !y && !z -> vec3Of(-vec.xd, -vec.yd, -vec.zd) to vec3Of(vec.xd, vec.yd, vec.zd)
            !x && !y && z -> vec3Of(-vec.xd, -vec.yd, vec.zd) to vec3Of(vec.xd, vec.yd, 0)
            !x && y && !z -> vec3Of(-vec.xd, vec.yd, -vec.zd) to vec3Of(vec.xd, 0, vec.zd)
            !x && y && z -> vec3Of(-vec.xd, vec.yd, vec.zd) to vec3Of(vec.xd, 0, 0)
            x && !y && !z -> vec3Of(vec.xd, -vec.yd, -vec.zd) to vec3Of(0, vec.yd, vec.zd)
            x && !y && z -> vec3Of(vec.xd, -vec.yd, vec.zd) to vec3Of(0, vec.yd, 0)
            x && y && !z -> vec3Of(vec.xd, vec.yd, -vec.zd) to vec3Of(0, 0, vec.zd)
            x && y && z -> vec3Of(vec.xd, vec.yd, vec.zd) to vec3Of(0, 0, 0)
            else -> error("x: $x, y: $y, z: $z")
        }
    }

    fun translateTexture(source: IModel, sel: ISelection, translation: IVector2): IModel {
        val transform = { it: IVector2 -> it + translation }

        return when (sel.selectionType) {
            SelectionType.OBJECT -> {
                val objRefs = sel.objects.toSet()
                return source.modifyObjects(objRefs) { _, obj ->
                    if (obj is IObjectCube) {
                        val newOffset = obj.textureOffset + translation * obj.textureSize
                        obj.withTextureOffset(newOffset)
                    } else {
                        val newMesh = transformMeshTexture(obj, obj.mesh.tex.indices.toSet(), transform)
                        obj.withMesh(newMesh)
                    }
                }
            }
            SelectionType.FACE -> transformTextureFaces(source, sel, transform)
            SelectionType.EDGE -> transformTextureEdges(source, sel, transform)
            SelectionType.VERTEX -> transformTextureVertex(source, sel, transform)
        }
    }

    fun rotateTexture(source: IModel, sel: ISelection, pivot: IVector2, rotation: Double): IModel {
        val matrix = TRSTransformation.fromRotationPivot(
                pivot.toVector3(0.0), vec3Of(0.0, 0.0, rotation)
        ).matrix.toJOML()

        val transform = { it: IVector2 -> matrix.transformVertex(it) }
        return when (sel.selectionType) {
            SelectionType.OBJECT -> transformTextureObjects(source, sel, transform)
            SelectionType.FACE -> transformTextureFaces(source, sel, transform)
            SelectionType.EDGE -> transformTextureEdges(source, sel, transform)
            SelectionType.VERTEX -> transformTextureVertex(source, sel, transform)
        }
    }

    fun scaleTexture(source: IModel, sel: ISelection, start: IVector2, end: IVector2, translation: IVector2, axis: IVector2): IModel {
        val size = end - start
        val offset = translation * axis / size
        val (_, move) = getScaleAndTranslation(axis.toVector3(0))
        val addition = -start + size * move.toVector2()

        println("addition: $addition, offset: $offset")
        println("ratio1: ${size.xd / size.yd}, ratio2: ${offset.xd / offset.yd}")
        val transform = { point: IVector2 ->
            point + (point + addition) * offset
        }
        return when (sel.selectionType) {
            SelectionType.OBJECT -> transformTextureObjects(source, sel, transform)
            SelectionType.FACE -> transformTextureFaces(source, sel, transform)
            SelectionType.EDGE -> transformTextureEdges(source, sel, transform)
            SelectionType.VERTEX -> transformTextureVertex(source, sel, transform)
        }
    }

    private fun applyTransformation(source: IModel, sel: ISelection, animator: Animator, func: (IObject, ITransformation) -> IObject): IModel {
        val objRefs = when (sel.selectionType) {
            SelectionType.OBJECT -> sel.objects.toSet()
            SelectionType.FACE -> sel.faces.map { it.toObjectRef() }.toSet()
            SelectionType.EDGE -> sel.edges.map { it.toObjectRef() }.toSet()
            SelectionType.VERTEX -> sel.pos.map { it.toObjectRef() }.toSet()
        }
        return source.modifyObjects(objRefs) { _, obj ->
            val mat = obj.getParentGlobalMatrix(source, animator)
            val inv = mat.toJOML().invert().toIMatrix()

            func(obj, TRSTransformation.fromMatrix(inv))
        }
    }

    private fun ITransformation.invert(): TRSTransformation {
        return TRSTransformation.fromMatrix(matrix.toJOML().invert().toIMatrix())
    }

    fun transformTextureObjects(source: IModel, sel: ISelection, transform: (IVector2) -> IVector2): IModel {
        val objRefs = sel.objects.toSet()
        return source.modifyObjects(objRefs) { _, obj ->
            val newMesh = transformMeshTexture(obj, obj.mesh.tex.indices.toSet(), transform)
            obj.withMesh(newMesh)
        }
    }

    fun transformTextureFaces(source: IModel, sel: ISelection, transform: (IVector2) -> IVector2): IModel {
        val objRefs = sel.faces.map { it.toObjectRef() }.toSet()
        return source.modifyObjects(objRefs) { ref, obj ->
            val indices: Set<Int> = sel.faces
                    .filter { it.toObjectRef() == ref }
                    .map { obj.mesh.faces[it.faceIndex] }
                    .flatMap { it.tex }
                    .toSet()

            val newMesh = transformMeshTexture(obj, indices, transform)
            obj.withMesh(newMesh)
        }
    }

    fun transformTextureEdges(source: IModel, sel: ISelection, transform: (IVector2) -> IVector2): IModel {
        val objRefs = sel.edges.map { it.toObjectRef() }.toSet()
        return source.modifyObjects(objRefs) { ref, obj ->
            val indices: Set<Int> = sel.edges
                    .filter { it.toObjectRef() == ref }
                    .flatMap { listOf(it.firstIndex, it.secondIndex) }
                    .toSet()

            val newMesh = transformMeshTexture(obj, indices, transform)
            obj.withMesh(newMesh)
        }
    }

    fun transformTextureVertex(source: IModel, sel: ISelection, transform: (IVector2) -> IVector2): IModel {
        val objRefs = sel.pos.map { it.toObjectRef() }.toSet()
        return source.modifyObjects(objRefs) { ref, obj ->
            val indices: Set<Int> = sel.pos
                    .filter { it.toObjectRef() == ref }
                    .map { it.posIndex }
                    .toSet()

            val newMesh = transformMeshTexture(obj, indices, transform)
            obj.withMesh(newMesh)
        }
    }

    fun transformMesh(obj: IObject, indices: Set<Int>, transform: IMatrix4): IMesh {

        val newPos: List<IVector3> = obj.mesh.pos.mapIndexed { index, it ->
            if (index in indices) transform.transformVertex(it) else it
        }
        return Mesh(pos = newPos, tex = obj.mesh.tex, faces = obj.mesh.faces)
    }

    fun transformMeshTexture(obj: IObject, indices: Set<Int>, transform: (IVector2) -> IVector2): IMesh {

        val newTex: List<IVector2> = obj.mesh.tex.mapIndexed { index, it ->
            if (index in indices) transform(it) else it
        }
        return Mesh(pos = obj.mesh.pos, tex = newTex, faces = obj.mesh.faces)
    }

    fun Matrix4d.transformVertex(it: IVector2): IVector2 {
        val vec4 = transform(Vector4d(it.xd, it.yd, 0.0, 1.0))
        return vec2Of(vec4.x, vec4.y)
    }

    fun splitTextures(model: IModel, selection: ISelection): IModel {

        return when (selection.selectionType) {
            SelectionType.OBJECT -> {
                model.modifyObjects(selection::isSelected) { _, obj ->
                    val faces = obj.mesh.faces
                    val pos = obj.mesh.pos

                    val newTex = faces.flatMap { it.getTextureVertex(obj.mesh) }

                    val newFaces = faces.mapIndexed { index, face ->
                        val startIndex = (index * 4)
                        val endIndex = ((index + 1) * 4)
                        val newTexIndices = (startIndex until endIndex).toList()

                        FaceIndex.from(face.pos, newTexIndices)
                    }

                    val newMesh = Mesh(pos, newTex, newFaces)
                    obj.withMesh(newMesh)
                }
            }
            else -> model
        }
    }

    fun scaleTextures(model: IModel, selection: ISelection, scale: Float): IModel {
        return when (selection.selectionType) {
            SelectionType.OBJECT -> {
                model.modifyObjects(selection::isSelected) { _, obj ->
                    if (obj is IObjectCube) {
                        obj.withTextureSize(obj.textureSize * scale)
                    } else {
                        val tex = obj.mesh.tex.map { it * scale }
                        val newMesh = Mesh(obj.mesh.pos, tex, obj.mesh.faces)

                        obj.withMesh(newMesh)
                    }
                }
            }
            else -> model
        }
    }
}

fun Matrix4d.transformVertex(it: IVector3): IVector3 {
    val vec4 = transform(Vector4d(it.xd, it.yd, it.zd, 1.0))
    return vec3Of(vec4.x, vec4.y, vec4.z)
}

fun IMatrix4.transformVertex(it: IVector3): IVector3 = toJOML().transformVertex(it)

