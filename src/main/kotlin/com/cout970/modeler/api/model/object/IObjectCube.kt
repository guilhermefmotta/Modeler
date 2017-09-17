package com.cout970.modeler.api.model.`object`

import com.cout970.modeler.core.model.TRSTransformation
import com.cout970.vector.api.IVector2
import com.cout970.vector.api.IVector3

/**
 * Created by cout970 on 2017/05/14.
 */
interface IObjectCube : IObject {
    val transformation: TRSTransformation
    val textureOffset: IVector2
    val textureSize: IVector2

    fun withSize(size: IVector3): IObjectCube
    fun withPos(pos: IVector3): IObjectCube
    fun withTransformation(transform: TRSTransformation): IObjectCube
    fun withTextureOffset(tex: IVector2): IObjectCube
}