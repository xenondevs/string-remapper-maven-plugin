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
@Mojo(name = "remap", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
class StringRemapper : AbstractMojo() {
    
    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject
    
    @Parameter(defaultValue = "\${project.build.directory}", readonly = true, required = true)
    private lateinit var buildDir: String
    
    @Component
    private lateinit var repoSystem: RepositorySystem
    
    @Parameter(defaultValue = "\${repositorySystemSession}", readonly = true, required = true)
    private lateinit var repoSession: RepositorySystemSession
    
    @Parameter(defaultValue = "\${project.remoteProjectRepositories}", readonly = true, required = true)
    private val repositories: List<RemoteRepository>? = null
    
    @Parameter(name = "mapsMojang", required = true)
    private lateinit var mapsMojang: String
    
    @Parameter(name = "mapsSpigot", required = true)
    private lateinit var mapsSpigot: String
    
    @Parameter(name = "mapsSpigotMembers", required = true)
    private lateinit var mapsSpigotMembers: String
    
    @Parameter(name = "remapGoal", required = true)
    private lateinit var remapGoal: String
    
    private val compileSourceRoots by lazy { project.compileSourceRoots as MutableList<String> }
    private val goal by lazy { Mappings.ResolveGoal.valueOf(remapGoal.uppercase()) }
    
    override fun execute() {
        log.info("Loading mappings...")
        loadMappings()
        log.info("Copying sources...")
        copySourceDirectories()
        log.info("Remapping...")
        performRemapping()
    }
    
    private fun loadMappings() {
        Mappings.loadMojangMappings(resolveArtifact(mapsMojang).file.bufferedReader())
        Mappings.loadSpigotMappings(
            resolveArtifact(mapsSpigot).file.bufferedReader(),
            resolveArtifact(mapsSpigotMembers).file.bufferedReader()
        )
    }
    
    private fun resolveArtifact(cords: String): Artifact {
        val artifact = DefaultArtifact(cords)
        val req = ArtifactRequest()
        req.artifact = artifact
        req.repositories = repositories
        return repoSystem.resolveArtifact(repoSession, req).artifact
    }
    
    private fun copySourceDirectories() {
        val baseDir = project.basedir
        val buildDir = File(buildDir)
        
        // Copy sources
        compileSourceRoots.withIndex().forEach { (i, path) ->
            val sourceRoot = File(path)
            val copiedSourceRoot = File(buildDir, "string-remapper-sources" + sourceRoot.absolutePath.removePrefix(baseDir.absolutePath))
            sourceRoot.copyRecursively(copiedSourceRoot)
            compileSourceRoots[i] = copiedSourceRoot.absolutePath
        }
    }
    
    private fun performRemapping() {
        compileSourceRoots
            .map(::File)
            .forEach { sourceRoot ->
                sourceRoot.walkTopDown().filter(File::isFile).forEach { file ->
                    log.debug("Remapping: ${file.absolutePath}")
                    FileRemapper(file).remap(goal)
                }
            }
    }
    
}