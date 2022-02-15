package xyz.xenondevs.stringremapper.visitor

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.stringremapper.Mappings.ResolveGoal
import java.util.concurrent.atomic.AtomicBoolean

class ClassRemapVisitor(visitor: ClassVisitor, private val goal: ResolveGoal, private val changed: AtomicBoolean) : ClassVisitor(Opcodes.ASM9, visitor) {
    
    override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val superVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        return StringVisitor(superVisitor, goal, changed)
    }
    
}