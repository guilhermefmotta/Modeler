package com.cout970.modeler.to_redo.newView.render.comp

import com.cout970.modeler.to_redo.model.api.IElementLeaf
import com.cout970.modeler.to_redo.model.util.getLeafElements
import com.cout970.modeler.to_redo.model.util.toAABB
import com.cout970.modeler.util.RenderUtil
import com.cout970.modeler.view.render.RenderContextOld
import org.lwjgl.opengl.GL11

/**
 * Created by cout970 on 2017/03/20.
 */
class AABBRenderComponent : IRenderableComponent {

    override fun render(ctx: RenderContextOld) {
        ctx.apply {
            if (controllerState.showBoundingBoxes.get()) {
                draw(GL11.GL_LINES, shaderHandler.formatPC, model.hashCode() xor 0xFFF) {
                    model.getLeafElements().map(IElementLeaf::toAABB).forEach {
                        RenderUtil.renderBox(this, it)
                    }
                }
            }
        }
    }
}