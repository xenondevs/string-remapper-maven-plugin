package xyz.xenondevs.stringremapper.visitor

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.stringremapper.Mappings
import xyz.xenondevs.stringremapper.Mappings.ResolveGoal
import java.util.concurrent.atomic.AtomicBoolean

class StringVisitor(visitor: MethodVisitor, private val goal: ResolveGoal, private val changed: AtomicBoolean) : MethodVisitor(Opcodes.ASM9, visitor) {
    
    override fun visitLdcInsn(value: Any?) {
        if (value is String) {
            val newValue = Mappings.processString(value, goal)
            if (newValue != value) {
                changed.set(true)
                super.visitLdcInsn(newValue)
                return
            }
        }
        super.visitLdcInsn(value)
    }
    
}