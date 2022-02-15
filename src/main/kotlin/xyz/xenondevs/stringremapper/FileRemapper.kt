package xyz.xenondevs.stringremapper

import org.apache.maven.plugin.logging.Log
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import xyz.xenondevs.stringremapper.Mappings.ResolveGoal
import xyz.xenondevs.stringremapper.visitor.ClassRemapVisitor
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class FileRemapper(private val file: File, private val log: Log) {
    
    fun remap(goal: ResolveGoal) {
        log.debug("Processing ${file.absolutePath}")
        val changed = AtomicBoolean()
        val reader = ClassReader(file.inputStream())
        val writer = ClassWriter(0)
        val visitor = ClassRemapVisitor(writer, goal, changed)
        reader.accept(visitor, 0)
        if (changed.get()) {
            log.info("Remapped strings in ${file.name}")
            file.writeBytes(writer.toByteArray())
        }
    }
    
}