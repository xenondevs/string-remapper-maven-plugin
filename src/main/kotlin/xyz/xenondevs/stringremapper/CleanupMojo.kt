package xyz.xenondevs.stringremapper

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "cleanup", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
class CleanupMojo : AbstractMojo() {
    
    @Parameter(defaultValue = "\${project.build.directory}", readonly = true, required = true)
    private lateinit var buildDir: String
    
    @Parameter(name = "srcOut", required = true)
    private lateinit var srcOut: List<String>
    
    override fun execute() {
        val buildDir = File(buildDir)
        val srcOut = srcOut.ifEmpty { listOf("string-remapper-sources") }
        srcOut.asSequence()
            .map { File(buildDir, it) }
            .forEach { it.deleteRecursively() }
    }
    
    
}