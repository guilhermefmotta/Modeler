package com.cout970.modeler.core.export.project

import com.cout970.modeler.api.animation.IAnimation
import com.cout970.modeler.api.animation.InterpolationMethod
import com.cout970.modeler.api.model.IModel
import com.cout970.modeler.api.model.ITransformation
import com.cout970.modeler.api.model.`object`.*
import com.cout970.modeler.api.model.material.IMaterial
import com.cout970.modeler.api.model.material.IMaterialRef
import com.cout970.modeler.api.model.mesh.IMesh
import com.cout970.modeler.api.model.selection.IObjectRef
import com.cout970.modeler.core.animation.Animation
import com.cout970.modeler.core.animation.Channel
import com.cout970.modeler.core.animation.Keyframe
import com.cout970.modeler.core.animation.ref
import com.cout970.modeler.core.export.*
import com.cout970.modeler.core.model.Model
import com.cout970.modeler.core.model.`object`.*
import com.cout970.modeler.core.model.material.ColoredMaterial
import com.cout970.modeler.core.model.material.MaterialNone
import com.cout970.modeler.core.model.material.TexturedMaterial
import com.cout970.modeler.core.model.mesh.FaceIndex
import com.cout970.modeler.core.model.mesh.Mesh
import com.cout970.modeler.core.model.toImmutable
import com.cout970.modeler.core.project.ProjectProperties
import com.cout970.modeler.core.resource.ResourcePath
import com.cout970.vector.api.IQuaternion
import com.cout970.vector.api.IVector2
import com.cout970.vector.api.IVector3
import com.google.gson.*
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import java.lang.reflect.Type
import java.net.URI
import java.util.*
import java.util.zip.ZipFile


object ProjectLoaderV12 {

    const val VERSION = "1.2"

    val gson = GsonBuilder()
            .setExclusionStrategies(ProjectExclusionStrategy())
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(UUID::class.java, UUIDSerializer())
            .registerTypeAdapter(IVector3::class.java, Vector3Serializer())
            .registerTypeAdapter(IVector2::class.java, Vector2Serializer())
            .registerTypeAdapter(IQuaternion::class.java, QuaternionSerializer())
            .registerTypeAdapter(IGroupRef::class.java, GroupRefSerializer())
            .registerTypeAdapter(IMaterialRef::class.java, MaterialRefSerializer())
            .registerTypeAdapter(IObjectRef::class.java, ObjectRefSerializer())
            .registerTypeAdapter(ITransformation::class.java, TransformationSerializer())
            .registerTypeAdapter(BiMultimap::class.java, BiMultimapSerializer())
            .registerTypeAdapter(ImmutableMap::class.java, ImmutableMapSerializer())
            .registerTypeAdapter(IModel::class.java, ModelSerializer())
            .registerTypeAdapter(IMaterial::class.java, MaterialSerializer())
            .registerTypeAdapter(IObject::class.java, ObjectSerializer())
            .registerTypeAdapter(IGroupTree::class.java, GroupTreeSerializer())
            .registerTypeAdapter(IGroup::class.java, serializerOf<Group>())
            .registerTypeAdapter(IMesh::class.java, MeshSerializer())
            .registerTypeAdapter(IAnimation::class.java, AnimationSerializer())
            .create()!!

    fun loadProject(zip: ZipFile, path: String): ProgramSave {

        val properties = zip.load<ProjectProperties>("project.json", gson)
                ?: throw IllegalStateException("Missing file 'project.json' inside '$path'")

        val model = zip.load<IModel>("model.json", gson)
                ?: throw IllegalStateException("Missing file 'model.json' inside '$path'")

        val animation = zip.load<IAnimation>("animation.json", gson)
                ?: throw IllegalStateException("Missing file 'animation.json' inside '$path'")

        checkIntegrity(listOf(model.objectMap, model.materialMap, model.groupMap, model.tree))
        checkIntegrity(listOf(animation))

        return ProgramSave(VERSION, properties, model.addAnimation(animation), emptyList())
    }

    class ModelSerializer : JsonDeserializer<IModel> {

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IModel {
            val obj = json.asJsonObject
            val objects: Map<IObjectRef, IObject> = context.deserializeT(obj["objectMap"])
            return Model.of(
                    objectMap = objects,
                    materialMap = context.deserializeT(obj["materialMap"]),
                    groupMap = context.deserializeT(obj["groupMap"]),
                    groupTree = MutableGroupTree(RootGroupRef, objects.keys.toMutableList()).toImmutable()
            )
        }
    }

    class MaterialSerializer : JsonDeserializer<IMaterial> {

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IMaterial {
            val obj = json.asJsonObject
            if (obj.has("type")) {
                return when (obj["type"].asString) {
                    "texture" -> {
                        val id = context.deserialize<UUID>(obj["id"], UUID::class.java)
                        TexturedMaterial(obj["name"].asString, ResourcePath(URI(obj["path"].asString)), id)
                    }
                    "color" -> {
                        val id = context.deserialize<UUID>(obj["id"], UUID::class.java)
                        ColoredMaterial(obj["name"].asString, context.deserializeT(obj["color"]), id)
                    }
                    else -> MaterialNone
                }
            }
            return when {
                obj["name"].asString == "noTexture" -> MaterialNone
                else -> {
                    val id = context.deserialize<UUID>(obj["id"], UUID::class.java)
                    TexturedMaterial(obj["name"].asString, ResourcePath(URI(obj["path"].asString)), id)
                }
            }
        }
    }

    class ObjectSerializer : JsonDeserializer<IObject> {

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IObject {
            val obj = json.asJsonObject
            return when (obj["class"].asString) {
                "ObjectCube" -> {
                    ObjectCube(
                            name = context.deserialize(obj["name"], String::class.java),
                            transformation = context.deserialize(obj["transformation"], ITransformation::class.java),
                            material = context.deserialize(obj["material"], IMaterialRef::class.java),
                            textureOffset = context.deserialize(obj["textureOffset"], IVector2::class.java),
                            textureSize = context.deserialize(obj["textureSize"], IVector2::class.java),
                            mirrored = context.deserialize(obj["mirrored"], Boolean::class.java),
                            visible = context.deserialize(obj["visible"], Boolean::class.java),
                            id = context.deserialize(obj["id"], UUID::class.java)
                    )
                }
                "Object" -> Object(
                        name = context.deserialize(obj["name"], String::class.java),
                        mesh = context.deserialize(obj["mesh"], IMesh::class.java),
                        material = context.deserialize(obj["material"], IMaterialRef::class.java),
                        visible = context.deserialize(obj["visible"], Boolean::class.java),
                        id = context.deserialize(obj["id"], UUID::class.java)
                )


                else -> throw IllegalStateException("Unknown Class: ${obj["class"]}")
            }
        }
    }

    class GroupTreeSerializer : JsonSerializer<IGroupTree>, JsonDeserializer<IGroupTree> {

        data class Aux(val key: IGroupRef, val value: Set<IGroupRef>)
        data class Aux2(val key: IGroupRef, val value: IGroupRef)

        override fun serialize(src: IGroupTree, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val tree = src as GroupTree
            return JsonObject().apply {
                add("childMap", JsonArray().also { array ->
                    tree.childMap.map { Aux(it.key, it.value) }.map { context.serializeT(it) }.forEach { it -> array.add(it) }
                })
                add("parentMap", JsonArray().also { array ->
                    tree.parentMap.map { Aux2(it.key, it.value) }.map { context.serializeT(it) }.forEach { it -> array.add(it) }
                })
            }
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IGroupTree {
            if (json.isJsonNull) return GroupTree.emptyTree()
            val obj = json.asJsonObject

            val childMapArray = obj["childMap"].asJsonArray
            val parentMapArray = obj["parentMap"].asJsonArray

            val childMap = childMapArray
                    .map { context.deserialize(it, Aux::class.java) as Aux }
                    .map { it.key to it.value }
                    .toMap()
                    .toImmutableMap()

            val parentMap = parentMapArray
                    .map { context.deserialize(it, Aux2::class.java) as Aux2 }
                    .map { it.key to it.value }
                    .toMap()
                    .toImmutableMap()

            return GroupTree(parentMap, childMap)
        }
    }

    class MeshSerializer : JsonSerializer<IMesh>, JsonDeserializer<IMesh> {

        override fun serialize(src: IMesh, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
            val pos = context.serializeT(src.pos)
            val tex = context.serializeT(src.tex)

            val faces = JsonArray().apply {

                src.faces.forEach { face ->
                    add(JsonArray().apply {

                        repeat(face.vertexCount) {
                            add(JsonArray().apply {
                                add(face.pos[it]); add(face.tex[it])
                            })
                        }
                    })
                }
            }

            return JsonObject().apply {
                add("pos", pos)
                add("tex", tex)
                add("faces", faces)
            }
        }

        override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): IMesh {
            val obj = json.asJsonObject

            val pos = context.deserializeT<List<IVector3>>(obj["pos"])
            val tex = context.deserializeT<List<IVector2>>(obj["tex"])

            val faces = obj["faces"].asJsonArray.map { face ->
                val vertex = face.asJsonArray
                val posIndices = ArrayList<Int>(vertex.size())
                val texIndices = ArrayList<Int>(vertex.size())

                repeat(vertex.size()) {
                    val pair = vertex[it].asJsonArray

                    posIndices.add(pair[0].asInt)
                    texIndices.add(pair[1].asInt)
                }

                FaceIndex.from(posIndices, texIndices)
            }

            return Mesh(pos, tex, faces)
        }
    }

    class AnimationSerializer : JsonDeserializer<IAnimation> {

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IAnimation {
            if (json.isJsonNull) return Animation.of()

            val obj = json.asJsonObject

            val channelsObj = obj["channels"]
            val channels = if (!channelsObj.isJsonArray) emptyList() else channelsObj.asJsonArray.map { it ->
                val channel = it.asJsonObject

                val interName = channel["interpolation"].asString
                val keyframesJson = channel["keyframes"].asJsonArray

                val keyframes = keyframesJson.map { it.asJsonObject }.map {
                    Keyframe(
                            time = it["time"].asFloat,
                            value = context.deserializeT(it["value"])
                    )
                }

                Channel(
                        name = channel["name"].asString,
                        interpolation = InterpolationMethod.valueOf(interName),
                        enabled = channel["enabled"].asBoolean,
                        keyframes = keyframes,
                        id = context.deserializeT(channel["id"])
                )
            }

            return Animation(
                    channels = channels.associateBy { it.ref },
                    timeLength = obj["timeLength"].asFloat,
                    channelMapping = emptyMap(),
                    name = "animation"
            )
        }
    }

    class BiMultimapSerializer : JsonDeserializer<BiMultimap<IGroupRef, IObjectRef>> {

        data class Aux(val key: IGroupRef, val value: List<IObjectRef>)

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BiMultimap<IGroupRef, IObjectRef> {
            if (json.isJsonNull || (json.isJsonArray && json.asJsonArray.size() == 0))
                return emptyBiMultimap()

            val array = json.asJsonArray
            val list = array.map { context.deserialize(it, Aux::class.java) as Aux }

            return biMultimapOf(*list.map { it.key to it.value }.toTypedArray())
        }
    }
}