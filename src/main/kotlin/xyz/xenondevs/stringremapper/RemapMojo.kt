package xyz.xenondevs.stringremapper

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import java.io.File

@Suppress("UNCHECKED_CAST")
@Mojo(name = "remap", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
class RemapMojo : AbstractMojo() {
    
    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject
    
    @Component
    private lateinit var repoSystem: RepositorySystem
    
    @Parameter(defaultValue = "\${repositorySystemSession}", readonly = true, required = true)
    private lateinit var repoSession: RepositorySystemSession
    
    @Parameter(defaultValue = "\${project.remoteProjectRepositories}", readonly = true, required = true)
    private val repositories: List<RemoteRepository>? = null
    
    @Parameter(name = "classesIn", required = false)
    private lateinit var classesIn: List<String>
    
    @Parameter(name = "mapsMojang", required = true)
    private lateinit var mapsMojang: String
    
    @Parameter(name = "mapsSpigot", required = true)
    private lateinit var mapsSpigot: List<String>
    
    @Parameter(name = "remapGoal", required = true)
    private lateinit var remapGoal: String
    
    private val goal by lazy { Mappings.ResolveGoal.valueOf(remapGoal.uppercase()) }
    
    override fun execute() {
        log.info("Loading mappings...")
        loadMappings()
        log.info("Remapping...")
        performRemapping()
    }
    
    private fun loadMappings() {
        Mappings.loadMojangMappings(resolveArtifact(mapsMojang).file.bufferedReader())
        mapsSpigot.map(::resolveArtifact).forEach { Mappings.loadSpigotMappings(it.file.bufferedReader()) }
    }
    
    private fun resolveArtifact(cords: String): Artifact {
        val artifact = DefaultArtifact(cords)
        val req = ArtifactRequest()
        req.artifact = artifact
        req.repositories = repositories
        return repoSystem.resolveArtifact(repoSession, req).artifact
    }
    
    private fun performRemapping() {
        classesIn.forEach { path ->
            val classesRoot = File(path)
            classesRoot.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
                log.debug("Remapping: ${file.absolutePath}")
                FileRemapper(file, log).remap(goal)
            }
        }
    }
    
}