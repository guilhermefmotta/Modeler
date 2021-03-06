package com.cout970.modeler.controller.tasks

import com.cout970.modeler.Program
import com.cout970.modeler.core.log.print
import com.cout970.modeler.gui.COMPUTE
import kotlinx.coroutines.experimental.launch

class TaskAsync(val callback: suspend ((ITask) -> Unit) -> Unit) : ITask {

    override fun run(state: Program) {
        launch(COMPUTE) {
            try {
                callback {
                    state.taskHistory.processTask(it)
                }
            } catch (e: Exception) {
                e.print()
            }
        }
    }
}