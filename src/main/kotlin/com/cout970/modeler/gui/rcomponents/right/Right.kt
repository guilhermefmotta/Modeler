package com.cout970.modeler.gui.rcomponents.right

import com.cout970.modeler.core.project.IProgramState
import com.cout970.modeler.gui.GuiState
import com.cout970.modeler.gui.leguicomp.classes
import com.cout970.modeler.input.event.IInput
import com.cout970.reactive.core.RBuilder
import com.cout970.reactive.core.RProps
import com.cout970.reactive.core.RStatelessComponent
import com.cout970.reactive.dsl.*
import com.cout970.reactive.nodes.child
import com.cout970.reactive.nodes.div
import com.cout970.reactive.nodes.style
import com.cout970.glutilities.device.Mouse as LibMouse


data class RightPanelProps(val visible: Boolean, val programState: IProgramState, val state: GuiState, val input: IInput) : RProps

class RightPanel : RStatelessComponent<RightPanelProps>() {

    override fun RBuilder.render() = div("RightPanel") {
        style {
            posY = 48f

            classes(if (!props.visible) "right_panel_hide" else "right_panel")
        }

        postMount {
            width = 288f
            posX = parent.width - width
            height = parent.size.y - 48f
        }

        div("Container") {

            style {
                transparent()
                borderless()
            }

            postMount {
                fillX()
                posY = 5f
                sizeY = parent.sizeY - posY
            }
            child(ModelTree::class, ModelTreeProps(props.programState, props.input))
            child(MaterialList::class, MaterialListProps(props.programState) { props.state.selectedMaterial })
        }
    }
}



