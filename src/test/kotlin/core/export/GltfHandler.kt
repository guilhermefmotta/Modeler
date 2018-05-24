package core.export

import com.cout970.modeler.core.export.ModelImporters.gltfImporter
import com.cout970.modeler.util.toResourcePath
import org.junit.Test
import java.io.File

/**
 * Created by cout970 on 2017/06/06.
 */

class GltfHandler {

    @Test
    fun `Try importing a cube mode`() {
        val path = File("src/test/resources/model/box.gltf").toResourcePath()

        val model = gltfImporter.import(path)

        println(model)
    }
}