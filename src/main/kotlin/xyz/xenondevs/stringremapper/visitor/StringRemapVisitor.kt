package xyz.xenondevs.stringremapper.visitor

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.stringremapper.Mappings
import xyz.xenondevs.stringremapper.Mappings.ResolveGoal
import java.util.concurrent.atomic.AtomicBoolean

private val REMAP_INSTRUCTION_PATTERN = Regex("""SR([CMF])\(([^"]*)\)""")

class StringVisitor(visitor: MethodVisitor, private val goal: ResolveGoal, private val changed: AtomicBoolean) : MethodVisitor(Opcodes.ASM9, visitor) {
    
    override fun visitLdcInsn(value: Any?) {
        var result: MatchResult? = null
        if (value is String && REMAP_INSTRUCTION_PATTERN.find(value)?.let { result = it } != null) {
            val lookupType = result!!.groups[1]!!.value
            val lookup = result!!.groups[2]!!.value
    
            val resolvedLookup = when (lookupType) {
                "C" -> Mappings.resolveClassLookup(lookup, goal)
                "M" -> Mappings.resolveMethodLookup(lookup, goal)
                "F" -> Mappings.resolveFieldLookup(lookup, goal)
                else -> throw UnsupportedOperationException()
            }
            changed.set(true)
            super.visitLdcInsn(resolvedLookup)
        } else {
            super.visitLdcInsn(value)
        }
    }
    
}