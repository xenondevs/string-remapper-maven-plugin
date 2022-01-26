package xyz.xenondevs.stringremapper

import java.io.File

private val REMAP_INSTRUCTION_PATTERN = Regex(""""SR([CMF])\(([^"]*)\)"""")

class FileRemapper(private val file: File) {
    
    fun remap(goal: Mappings.ResolveGoal) {
        var text = file.readText()
        var hasChanged = false
        
        var result: MatchResult? = null
        while (REMAP_INSTRUCTION_PATTERN.find(text)?.let { result = it } != null) {
            val lookupType = result!!.groups[1]!!.value
            val lookup = result!!.groups[2]!!.value
            
            val resolvedLookup = when (lookupType) {
                "C" -> Mappings.resolveClassLookup(lookup, goal)
                "M" -> Mappings.resolveMethodLookup(lookup, goal)
                "F" -> Mappings.resolveFieldLookup(lookup, goal)
                else -> throw UnsupportedOperationException()
            }
            
            text = text.replaceRange(result!!.range, "\"$resolvedLookup\"")
            
            hasChanged = true
        }
        
        if (hasChanged) file.writeText(text)
    }
    
}