package com.cout970.modeler

import com.cout970.glutilities.structure.Timer
import com.cout970.glutilities.window.GLFWLoader
import com.cout970.modeler.controller.ActionExecutor
import com.cout970.modeler.core.config.ConfigManager
import com.cout970.modeler.core.export.ExportManager
import com.cout970.modeler.core.log.Level
import com.cout970.modeler.core.log.log
import com.cout970.modeler.core.log.print
import com.cout970.modeler.core.project.ProjectManager
import com.cout970.modeler.core.resource.ResourceLoader
import com.cout970.modeler.view.GuiInitializer
import com.cout970.modeler.view.event.EventController
import com.cout970.modeler.view.render.RenderManager
import com.cout970.modeler.view.window.Loop
import com.cout970.modeler.view.window.WindowHandler
import java.io.File

/**
 * Created by cout970 on 2016/11/29.
 */
class Initializer {

    fun init(programArguments: List<String>): ProgramSate {

        Debugger.setInit(this)

        log(Level.FINE) { "Loading config" }
        ConfigManager.loadConfig()
        log(Level.FINE) { "Config loaded" }

        log(Level.FINE) { "Creating ResourceLoader" }
        val resourceLoader = ResourceLoader()
        log(Level.FINE) { "Creating Timer" }
        val timer = Timer()
        log(Level.FINE) { "Creating WindowHandler" }
        val windowHandler = WindowHandler(timer)
        log(Level.FINE) { "Creating EventController" }
        val eventController = EventController()
        log(Level.FINE) { "Creating ProjectController" }
        val projectManager = ProjectManager()
        log(Level.FINE) { "Creating ModelTransformer" }
        val modelTransformer = ActionExecutor(projectManager)
        log(Level.FINE) { "Creating ExportManager" }
        val exportManager = ExportManager(resourceLoader)

        log(Level.FINE) { "Creating RenderManager" }
        val renderManager = RenderManager()

        log(Level.FINE) { "Creating GuiInitializer" }
        val gui = GuiInitializer(
                eventController,
                windowHandler,
                renderManager,
                resourceLoader,
                timer,
                projectManager,
                modelTransformer
        ).init()

        log(Level.FINE) { "Creating Loop" }
        val mainLoop = Loop(listOf(renderManager, gui.listeners, eventController, windowHandler, modelTransformer),
                timer, windowHandler::shouldClose)

        parseArgs(programArguments, exportManager, projectManager)

        val state = ProgramSate(
                resourceLoader = resourceLoader,
                windowHandler = windowHandler,
                eventController = eventController,
                renderManager = renderManager,
                mainLoop = mainLoop,
                exportManager = exportManager,
                gui = gui,
                projectManager = projectManager,
                actionExecutor = modelTransformer
        )

        gui.selectionHandler.listeners.add(gui.guiUpdater::onSelectionUpdate)

        log(Level.FINE) { "Starting GLFW" }
        GLFWLoader.init()
        log(Level.FINE) { "Starting GLFW window" }
        windowHandler.create()
        log(Level.FINE) { "Binding listeners and callbacks to window" }
        eventController.bindWindow(windowHandler.window)

        log(Level.FINE) { "Initializing renderers" }
        renderManager.initOpenGl(resourceLoader, windowHandler, eventController)
        log(Level.FINE) { "Registering Input event listeners" }
        gui.listeners.initListeners(eventController, gui)
        gui.commandExecutor.programState = state

        gui.resources.reload(resourceLoader)

        log(Level.FINE) { "Searching for last project" }
        exportManager.loadLastProjectIfExists(projectManager)
        gui.commandExecutor.execute("gui.left.refresh")
        log(Level.FINE) { "Initialization done" }
        return state
    }

    private fun parseArgs(programArguments: List<String>, exportManager: ExportManager,
                          projectManager: ProjectManager) {
        if (programArguments.isNotEmpty()) {
            log(Level.FINE) { "Parsing arguments..." }
            if (File(programArguments[0]).exists()) {
                try {
                    log(Level.NORMAL) { "Loading Project at '${programArguments[0]}'" }
                    val project = exportManager.loadProject(programArguments[0])
                    projectManager.loadProject(project)
                    log(Level.NORMAL) { "Project loaded" }
                } catch (e: Exception) {
                    log(Level.ERROR) { "Unable to load project file at '${programArguments[0]}'" }
                    e.print()
                }
            } else {
                log(Level.ERROR) { "Invalid program argument: '${programArguments[0]}' is not a valid path to a save file" }
            }
            log(Level.FINE) { "Parsing arguments done" }
        } else {
            log(Level.FINE) { "No program arguments found, ignoring..." }
        }
    }

    fun start(program: ProgramSate) {
        log(Level.FINE) { "Starting loop" }
        program.mainLoop.run()
        log(Level.FINE) { "Ending loop" }
        stop()
    }

    private fun stop() {
        GLFWLoader.terminate()
    }
}