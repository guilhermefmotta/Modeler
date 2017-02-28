package com.cout970.modeler.util

import com.cout970.matrix.extensions.Matrix4
import com.cout970.modeler.model.Model
import com.cout970.modeler.model.api.IElementLeaf
import com.cout970.modeler.model.util.getElement
import com.cout970.modeler.model.util.getLeafPaths
import com.cout970.modeler.model.util.rayTrace
import com.cout970.modeler.selection.ElementPath
import com.cout970.modeler.selection.VertexPath
import com.cout970.raytrace.Ray
import com.cout970.raytrace.RayTraceResult
import com.cout970.raytrace.RayTraceUtil
import com.cout970.vector.api.IVector2
import com.cout970.vector.api.IVector3
import com.cout970.vector.extensions.*

/**
 * Created by cout970 on 2017/02/27.
 */
object Raytracer {

    fun raytraceElements(ray: Ray, model: Model): Pair<RayTraceResult, ElementPath>? {
        val hits = mutableListOf<Pair<RayTraceResult, ElementPath>>()

        model.getLeafPaths().forEach { path ->
            val obj = model.getElement(path) as IElementLeaf

            obj.rayTrace(Matrix4.IDENTITY, ray)?.let {
                hits += it to path
            }
        }
        return hits.getClosest(ray)
    }

    fun raytraceVertexPos(ray: Ray, model: Model, vertexSize: Float): Pair<RayTraceResult, VertexPath>? {
        val hits = mutableListOf<Pair<RayTraceResult, VertexPath>>()

        model.getLeafPaths().forEach { path ->
            val obj = model.getElement(path) as IElementLeaf

            obj.positions.forEachIndexed { index, pos ->
                val start = pos - vec3Of(0.125) * vertexSize
                val end = pos + vec3Of(0.125) * vertexSize

                RayTraceUtil.rayTraceBox3(start, end, ray, FakeRayObstacle)?.let {
                    hits += it to VertexPath(path, index)
                }
            }
        }
        return hits.getClosest(ray)
    }

    fun raytraceEdgePos(ray: Ray, model: Model, vertexSize: Float)
            : Pair<RayTraceResult, Pair<VertexPath, VertexPath>>? {

        val hits = mutableListOf<Pair<RayTraceResult, Pair<VertexPath, VertexPath>>>()

        model.getLeafPaths().forEach { path ->
            val obj = model.getElement(path) as IElementLeaf

            obj.faces.forEachIndexed { _, quadIndex ->
                quadIndex.edges.forEach { edgeIndex ->
                    val edge = edgeIndex.toEdge(obj)
                    val min = edge.a.pos min edge.b.pos
                    val max = edge.a.pos max edge.b.pos

                    val start = min - vec3Of(0.125) * vertexSize
                    val end = max + vec3Of(0.125) * vertexSize

                    RayTraceUtil.rayTraceBox3(start, end, ray, FakeRayObstacle)?.let {
                        hits += it to Pair(VertexPath(path, edgeIndex.a.first), VertexPath(path, edgeIndex.b.first))
                    }
                }
            }
        }
        return hits.getClosest(ray)
    }

    fun raytraceQuadPos(ray: Ray, model: Model): Pair<RayTraceResult, List<VertexPath>>? {

        val hits = mutableListOf<Pair<RayTraceResult, List<VertexPath>>>()

        model.getLeafPaths().forEach { path ->
            val obj = model.getElement(path) as IElementLeaf

            obj.faces.forEach { quadIndex ->
                val pos = quadIndex.pos.map { obj.positions[it] }

                RayTraceUtil.rayTraceQuad(ray, FakeRayObstacle, pos[0], pos[1], pos[2], pos[3])?.let {
                    hits += it to listOf(
                            VertexPath(path, quadIndex.pos[0]),
                            VertexPath(path, quadIndex.pos[1]),
                            VertexPath(path, quadIndex.pos[2]),
                            VertexPath(path, quadIndex.pos[3])
                    )
                }
            }
        }
        return hits.getClosest(ray)
    }

    fun raytraceQuadTex(ray: Ray, model: Model, to3D: (IVector2) -> IVector3): Pair<RayTraceResult, List<VertexPath>>? {

        val hits = mutableListOf<Pair<RayTraceResult, List<VertexPath>>>()

        model.getLeafPaths().forEach { path ->
            val obj = model.getElement(path) as IElementLeaf

            obj.faces.forEach { quadIndex ->
                val pos = quadIndex.tex.map { obj.textures[it] }.map(to3D)

                RayTraceUtil.rayTraceQuad(ray, FakeRayObstacle, pos[0], pos[1], pos[2], pos[3])?.let {
                    hits += it to listOf(
                            VertexPath(path, quadIndex.tex[0]),
                            VertexPath(path, quadIndex.tex[1]),
                            VertexPath(path, quadIndex.tex[2]),
                            VertexPath(path, quadIndex.tex[3])
                    )
                }
            }
        }
        return hits.getClosest(ray)
    }
}