package com.cout970.modeler.core.record.action

/**
 * Created by cout970 on 2016/12/07.
 *
 * Implementations of the interface must be idempotent
 */
//TODO read all the actions
interface IAction {

    fun run()

    fun undo()
}