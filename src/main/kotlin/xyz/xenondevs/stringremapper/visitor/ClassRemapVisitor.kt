package xyz.xenondevs.stringremapper.visitor

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.stringremapper.Mappings
import java.util.concurrent.atomic.AtomicBoolean

class ClassRemapVisitor(visitor: ClassVisitor, private val goal: Mappings.ResolveGoal, private val changed: AtomicBoolean) : ClassVisitor(Opcodes.ASM9, visitor) {
    
    override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val superVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        return StringVisitor(superVisitor, goal, changed)
    }
    
    override fun visitField(access: Int, name: String?, descriptor: String?, signature: String?, value: Any?): FieldVisitor {
        if (value is String) {
            val newValue = Mappings.processString(value, goal)
            if (newValue != value) {
                changed.set(true)
                return super.visitField(access, name, descriptor, signature, newValue)
            }
        }
        return super.visitField(access, name, descriptor, signature, value)
    }
    
}