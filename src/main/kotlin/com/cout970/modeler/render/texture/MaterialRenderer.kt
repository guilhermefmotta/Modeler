package com.cout970.modeler.render.texture

import com.cout970.glutilities.structure.GLStateMachine
import com.cout970.glutilities.tessellator.BufferPTNC
import com.cout970.glutilities.tessellator.DrawMode
import com.cout970.matrix.extensions.Matrix4
import com.cout970.matrix.extensions.times
import com.cout970.modeler.api.model.IModel
import com.cout970.modeler.api.model.material.IMaterial
import com.cout970.modeler.api.model.material.IMaterialRef
import com.cout970.modeler.api.model.selection.*
import com.cout970.modeler.core.config.Config
import com.cout970.modeler.core.model.selection.ObjectRef
import com.cout970.modeler.render.tool.AutoCache
import com.cout970.modeler.render.tool.CacheFlags
import com.cout970.modeler.render.tool.RenderContext
import com.cout970.modeler.render.tool.useBlend
import com.cout970.modeler.util.MatrixUtils
import com.cout970.vector.api.IVector3
import com.cout970.vector.extensions.*
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Created by cout970 on 2017/07/11.
 */
class MaterialRenderer {

    var areasCache = AutoCache(CacheFlags.MODEL, CacheFlags.MATERIAL, CacheFlags.VISIBILITY,
            CacheFlags.SELECTION_TEXTURE, CacheFlags.TEXTURE_CURSOR)
    val gridLines = AutoCache(CacheFlags.MATERIAL)
    val materialCache = AutoCache(CacheFlags.MATERIAL)
    val selectionCache = AutoCache(CacheFlags.MODEL, CacheFlags.SELECTION_TEXTURE, CacheFlags.MATERIAL)

    val cursorRenderer = TextureCursorRenderer()

    fun renderWorld(ctx: RenderContext, ref: IMaterialRef, material: IMaterial) {
        setCamera(ctx)
        GLStateMachine.depthTest.disable()

        renderMaterial(ctx, material)

        if (ctx.gui.state.drawTextureGridLines) {
            renderGridLines(ctx, material)
        }

        if (ctx.gui.state.drawTextureProjection) {
            GLStateMachine.useBlend(0.5f) {
                renderMappedAreas(ctx, ref, material)
            }
        }

        ctx.gui.modelAccessor.textureSelectionHandler.getSelection().ifNotNull {
            renderSelection(ctx, it, material)
        }

        GLStateMachine.depthTest.enable()

        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
        cursorRenderer.renderCursor(ctx)
    }

    fun renderMappedAreas(ctx: RenderContext, ref: IMaterialRef, material: IMaterial) {
        val vao = areasCache.getOrCreate(ctx) {
            val model = ctx.gui.modelAccessor.model
            val objs = model.objectRefs
                    .filter { model.isVisible(it) }
                    .map { model.getObject(it) }
                    .filter { it.material == ref }

            ctx.buffer.build(DrawMode.QUADS) {
                objs.forEach { obj ->
                    val mesh = obj.mesh

                    mesh.faces.forEachIndexed { index, face ->
                        val vec = Color.getHSBColor((index * 59 % 360) / 360f, 0.5f, 1.0f)
                        val color = vec3Of(vec.red, vec.green, vec.blue) / 255

                        val positions = face.tex
                                .map { mesh.tex[it] }
                                .map { vec2Of(it.xd, 1 - it.yd) }
                                .map { it * material.size }
                        positions.indices.forEach {
                            val pos0 = positions[it]
                            add(vec3Of(pos0.x, pos0.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                        }
                    }
                }
            }
        }
        GL11.glLineWidth(2f)
        ctx.shader.apply {
            useColor.setInt(1)
            useLight.setInt(0)
            useTexture.setInt(0)
            matrixM.setMatrix4(Matrix4.IDENTITY)
            accept(vao)
        }
        GL11.glLineWidth(1f)
    }

    fun renderSelection(ctx: RenderContext, selection: ISelection, material: IMaterial) {
        val vao = selectionCache.getOrCreate(ctx) {
            val model = ctx.gui.state.tmpModel ?: ctx.gui.modelAccessor.model
            val color = Config.colorPalette.textureSelectionColor

            ctx.buffer.build(DrawMode.LINES) {

                when (selection.selectionType) {
                    SelectionType.OBJECT -> this.renderObjectSelection(model, selection, material, color)
                    SelectionType.FACE -> this.renderFaceSelection(model, selection, material, color)
                    SelectionType.EDGE -> this.renderEdgeSelection(model, selection, material, color)
                    SelectionType.VERTEX -> this.renderVertexSelection(model, selection, material, color)
                }
            }
        }

        GL11.glLineWidth(2f)
        ctx.shader.apply {
            useColor.setInt(1)
            useLight.setInt(0)
            useTexture.setInt(0)
            matrixM.setMatrix4(Matrix4.IDENTITY)
            accept(vao)
        }
        GL11.glLineWidth(1f)
    }

    private fun BufferPTNC.renderVertexSelection(model: IModel, selection: ISelection, material: IMaterial,
                                                 color: IVector3) {
        val refs = selection.refs.filterIsInstance<IPosRef>()
        refs.forEach { ref ->
            val obj = model.getObject(ObjectRef(ref.objectIndex))

            val pos = ref.posIndex
                    .let { obj.mesh.tex[it] }
                    .let { vec2Of(it.xd, 1 - it.yd) }
                    .let { it * material.size }

            val pos0 = pos + vec2Of(-1 / 128.0, -1 / 128.0)
            val pos1 = pos + vec2Of(1 / 128.0, -1 / 128.0)
            val pos2 = pos + vec2Of(1 / 128.0, 1 / 128.0)
            val pos3 = pos + vec2Of(-1 / 128.0, 1 / 128.0)

            add(vec3Of(pos0.x, pos0.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
            add(vec3Of(pos1.x, pos1.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
            add(vec3Of(pos2.x, pos2.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
            add(vec3Of(pos3.x, pos3.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
        }
    }

    private fun BufferPTNC.renderEdgeSelection(model: IModel, selection: ISelection, material: IMaterial,
                                               color: IVector3) {
        val refs = selection.refs.filterIsInstance<IEdgeRef>()
        refs.forEach { ref ->
            val obj = model.getObject(ObjectRef(ref.objectIndex))

            val positions = listOf(ref.firstIndex, ref.secondIndex)
                    .map { obj.mesh.tex[it] }
                    .map { vec2Of(it.xd, 1 - it.yd) }
                    .map { it * material.size }

            val pos0 = positions[0]
            val pos1 = positions[1]
            add(vec3Of(pos0.x, pos0.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
            add(vec3Of(pos1.x, pos1.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
        }
    }

    private fun BufferPTNC.renderFaceSelection(model: IModel, selection: ISelection, material: IMaterial,
                                               color: IVector3) {
        val refs = selection.refs.filterIsInstance<IFaceRef>()
        refs.forEach { ref ->
            val obj = model.getObject(ObjectRef(ref.objectIndex))
            val face = obj.mesh.faces[ref.faceIndex]

            val positions = face.tex
                    .map { obj.mesh.tex[it] }
                    .map { vec2Of(it.xd, 1 - it.yd) }
                    .map { it * material.size }

            positions.indices.forEach {
                val next = (it + 1) % positions.size
                val pos0 = positions[it]
                val pos1 = positions[next]
                add(vec3Of(pos0.x, pos0.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                add(vec3Of(pos1.x, pos1.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
            }
        }
    }

    private fun BufferPTNC.renderObjectSelection(model: IModel, selection: ISelection, material: IMaterial,
                                                 color: IVector3) {
        val objs = model.objectRefs
                .filter { selection.isSelected(it) }
                .map { model.getObject(it) }

        objs.forEach { obj ->
            val mesh = obj.mesh

            mesh.faces.forEach { face ->

                val positions = face.tex
                        .map { mesh.tex[it] }
                        .map { vec2Of(it.xd, 1 - it.yd) }
                        .map { it * material.size }

                positions.indices.forEach {
                    val next = (it + 1) % positions.size
                    val pos0 = positions[it]
                    val pos1 = positions[next]
                    add(vec3Of(pos0.x, pos0.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                    add(vec3Of(pos1.x, pos1.y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                }
            }
        }
    }

    fun renderMaterial(ctx: RenderContext, material: IMaterial) {
        val vao = materialCache.getOrCreate(ctx) {
            ctx.buffer.build(DrawMode.QUADS) {
                val maxX = material.size.xi
                val maxY = material.size.yi
                add(vec3Of(0, 0, 0), vec2Of(0, 1), Vector3.ORIGIN, Vector3.ORIGIN)
                add(vec3Of(maxX, 0, 0), vec2Of(1, 1), Vector3.ORIGIN, Vector3.ORIGIN)
                add(vec3Of(maxX, maxY, 0), vec2Of(1, 0), Vector3.ORIGIN, Vector3.ORIGIN)
                add(vec3Of(0, maxY, 0), vec2Of(0, 0), Vector3.ORIGIN, Vector3.ORIGIN)
            }
        }

        material.bind()
        ctx.shader.apply {
            useColor.setBoolean(false)
            useLight.setBoolean(false)
            useTexture.setBoolean(true)
            matrixM.setMatrix4(Matrix4.IDENTITY)
            accept(vao)
        }
    }

    fun renderGridLines(ctx: RenderContext, material: IMaterial) {
        val vao = gridLines.getOrCreate(ctx) {
            ctx.buffer.build(DrawMode.LINES) {
                val min = 0
                val maxX = material.size.xi
                val maxY = material.size.yi

                for (x in min..maxX) {
                    val color = if (x % 16 == 0) Config.colorPalette.grid2Color else Config.colorPalette.grid1Color
                    add(vec3Of(x, min, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                    add(vec3Of(x, maxY, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                }
                for (y in min..maxY) {
                    val color = if (y % 16 == 0) Config.colorPalette.grid2Color else Config.colorPalette.grid1Color
                    add(vec3Of(min, y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                    add(vec3Of(maxX, y, 0), Vector2.ORIGIN, Vector3.ORIGIN, color)
                }
            }
        }
        ctx.shader.apply {
            useColor.setInt(1)
            useLight.setInt(0)
            useTexture.setInt(0)
            matrixM.setMatrix4(Matrix4.IDENTITY)
            accept(vao)
        }
    }

    fun setCamera(ctx: RenderContext) {
        val projection = MatrixUtils.createOrthoMatrix(ctx.viewport)
        val view = ctx.camera.matrixForUV
        ctx.shader.matrixVP.setMatrix4(projection * view)
    }
}