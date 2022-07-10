package xyz.xenondevs.stringremapper

import org.objectweb.asm.Type
import java.io.BufferedReader
import java.io.Reader

private val MOJANG_START_CLASS_PATTERN = Regex("""^([\w.$]*) -> ([\w.$]*):$""")
private val MOJANG_DEFINE_METHOD_PATTERN = Regex("""^ {4}[\d:]*([\w.$\[\]]*) ([\w$]*)\(([\w*.,$\[\]]*)\) -> ([\w$]*)$""")
private val MOJANG_DEFINE_FIELD_PATTERN = Regex("""^ {4}([\w.$\[\]]*) ([\w_$\[\]]*) -> ([\w$]*)$""")

private val SPIGOT_DEFINE_CLASS_PATTERN = Regex("""^([\w/$]*) ([\w/$]*)$""")
private val SPIGOT_DEFINE_METHOD_PATTERN = Regex("""^([\w/$]*) ([\w$]*) (\([\w/$\[\];]*\)[\w/$\[\];]*) ([\w/$;]*)$""")
private val SPIGOT_DEFINE_FIELD_PATTERN = Regex("""^([\w/$]*) ([\w$]*) ([\w$]*)$""")

private val CLASS_MAPPINGS_LOOKUP_PATTERN = Regex("""^([\w.$]*)$""")
private val METHOD_MAPPINGS_LOOKUP_PATTERN = Regex("""^([\w.$]*) ([\w.$\[\]]*) ([\w$]*)\(([\w.$\[\]]*)\)$""")
private val LIGHT_METHOD_MAPPINGS_LOOKUP_PATTERN = Regex("""^([\w.$]*) ([\w$]*)$""")
private val FIELD_MAPPINGS_LOOKUP_PATTERN = Regex("""^([\w.$]*) ([\w.$\[\]]*) ([\w_$]*)$""")
private val LIGHT_FIELD_MAPPINGS_LOOKUP_PATTERN = Regex("""^([\w.$]*) ([\w_$]*)$""")

private val REMAP_INSTRUCTION_PATTERN = Regex("""SR([CMF])(/)?\(([^")]*)\)""")

object Mappings {
    
    private val mojangMappings = HashMap<String, ClassMappings>()
    private val spigotMappings = HashMap<String, ClassMappings>()
    
    private val reverseSpigotMappings = HashMap<String, ClassMappings>()
    
    fun loadMojangMappings(reader: BufferedReader) {
        var classMappings: ClassMappings? = null
        reader.useLinesForEach { line ->
            if (line.startsWith('#') || line.isBlank()) return@useLinesForEach
            
            val startClassResult = MOJANG_START_CLASS_PATTERN.find(line)
            
            if (startClassResult != null) {
                val originalName = startClassResult.groups[1]!!.value
                val obfuscatedName = startClassResult.groups[2]!!.value
                
                ClassMappings(obfuscatedName).also {
                    mojangMappings[originalName] = it
                    classMappings = it
                }
                
                return@useLinesForEach
            }
            
            val currentClassMappings = classMappings ?: return@useLinesForEach
            
            val defineFieldResult = MOJANG_DEFINE_FIELD_PATTERN.find(line)
            if (defineFieldResult != null) {
                val mojangFieldClassName = defineFieldResult.groups[1]!!.value
                val mojangFieldName = defineFieldResult.groups[2]!!.value
                val obfuscatedFieldName = defineFieldResult.groups[3]!!.value
                
                val identifier = FieldIdentifier(
                    mojangFieldName,
                    mojangFieldClassName
                )
                
                currentClassMappings.fieldMappings[identifier] = obfuscatedFieldName
                currentClassMappings.lightFieldMappings[mojangFieldName] = obfuscatedFieldName
                
                return@useLinesForEach
            }
            
            val defineMethodResult = MOJANG_DEFINE_METHOD_PATTERN.find(line)
            if (defineMethodResult != null) {
                val mojangReturnTypeClassName = defineMethodResult.groups[1]!!.value
                val mojangMethodName = defineMethodResult.groups[2]!!.value
                val mojangParameters = defineMethodResult.groups[3]!!.value
                val obfuscatedMethodName = defineMethodResult.groups[4]!!.value
                
                val identifier = MethodIdentifier(
                    mojangMethodName,
                    mojangReturnTypeClassName,
                    mojangParameters
                )
                
                currentClassMappings.methodMappings[identifier] = obfuscatedMethodName
                currentClassMappings.lightMethodMappings[mojangMethodName] = obfuscatedMethodName
                
                return@useLinesForEach
            }
        }
    }
    
    fun loadSpigotMappings(reader: BufferedReader) {
        reader.useLinesForEach { line ->
            if (line.startsWith('#') || line.isBlank()) return@useLinesForEach
            
            val defineClassResult = SPIGOT_DEFINE_CLASS_PATTERN.find(line)
            if (defineClassResult != null) {
                val obfuscatedClassName = defineClassResult.groups[1]!!.value
                val spigotMappedClassName = defineClassResult.groups[2]!!.value
                val classMappings = ClassMappings(spigotMappedClassName.replace('/', '.'))
                spigotMappings[obfuscatedClassName] = classMappings
                reverseSpigotMappings[spigotMappedClassName] = classMappings
                
                return@useLinesForEach
            }
            
            val defineMethodResult = SPIGOT_DEFINE_METHOD_PATTERN.find(line)
            if (defineMethodResult != null) {
                val className = defineMethodResult.groups[1]!!.value
                val obfuscatedName = defineMethodResult.groups[2]!!.value
                val descriptor = Type.getType(defineMethodResult.groups[3]!!.value)
                val spigotMappedName = defineMethodResult.groups[4]!!.value
                
                val classMappings = reverseSpigotMappings[className] ?: return@useLinesForEach
                
                val returnType = descriptor.returnType.className
                val parameters = descriptor.argumentTypes.map(Type::getClassName)
                val identifier = MethodIdentifier(
                    obfuscatedName,
                    returnType,
                    parameters.joinToString(",")
                )
                
                classMappings.methodMappings[identifier] = spigotMappedName
                classMappings.lightMethodMappings[obfuscatedName] = spigotMappedName
                
                return@useLinesForEach
            }
            
            val defineFieldResult = SPIGOT_DEFINE_FIELD_PATTERN.find(line)
            if (defineFieldResult != null) {
                val className = defineFieldResult.groups[1]!!.value
                val obfuscatedName = defineFieldResult.groups[2]!!.value
                val spigotMappedName = defineFieldResult.groups[3]!!.value
                val classMappings = reverseSpigotMappings[className] ?: return@useLinesForEach
                classMappings.lightFieldMappings[obfuscatedName] = spigotMappedName
                
                return@useLinesForEach
            }
        }
    }
    
    fun resolveClassLookup(lookup: String, goal: ResolveGoal): String? {
        val classLookupResult = CLASS_MAPPINGS_LOOKUP_PATTERN.find(lookup)
        if (classLookupResult != null) {
            val mojangClassName = classLookupResult.groups[1]!!.value
            
            return when (goal) {
                ResolveGoal.MOJANG -> mojangClassName
                ResolveGoal.OBFUSCATED -> mojangMappings[mojangClassName]!!.name
                ResolveGoal.SPIGOT, ResolveGoal.SPIGOT_MEMBERS -> getSpigotClassName(mojangClassName)
            }
        }
        
        return null
    }
    
    fun resolveMethodLookup(lookup: String, goal: ResolveGoal): String? {
        val lightMethodLookupResult = LIGHT_METHOD_MAPPINGS_LOOKUP_PATTERN.find(lookup)
        if (lightMethodLookupResult != null) {
            val mojangOwnerClassName = lightMethodLookupResult.groups[1]!!.value
            val mojangMethodName = lightMethodLookupResult.groups[2]!!.value
            
            if (goal == ResolveGoal.MOJANG) return mojangMethodName
            
            val mojangClassMappings = mojangMappings[mojangOwnerClassName]!!
            val obfuscatedMethodName = mojangClassMappings.lightMethodMappings[mojangMethodName]!!
            
            if (goal == ResolveGoal.OBFUSCATED || goal == ResolveGoal.SPIGOT) return obfuscatedMethodName
            
            val spigotClassMappings = spigotMappings[mojangClassMappings.name]!!
            return spigotClassMappings.lightMethodMappings[obfuscatedMethodName] ?: obfuscatedMethodName
        }
        
        val methodLookupResult = METHOD_MAPPINGS_LOOKUP_PATTERN.find(lookup)
        if (methodLookupResult != null) {
            val mojangOwnerClassName = methodLookupResult.groups[1]!!.value
            val mojangReturnTypeClassName = methodLookupResult.groups[2]!!.value
            val mojangMethodName = methodLookupResult.groups[3]!!.value
            val mojangMethodParameters = methodLookupResult.groups[4]!!.value
            
            if (goal == ResolveGoal.MOJANG) return mojangMethodName
            
            val mojangClassMappings = mojangMappings[mojangOwnerClassName]!!
            val obfuscatedMethodName = mojangClassMappings.methodMappings[MethodIdentifier(mojangMethodName, mojangReturnTypeClassName, mojangMethodParameters)]!!
            
            if (goal == ResolveGoal.OBFUSCATED || goal == ResolveGoal.SPIGOT) return obfuscatedMethodName
            
            val spigotReturnTypeClassName = getSpigotClassName(mojangReturnTypeClassName)
            val spigotMethodParameters = mojangMethodParameters.split(',').joinToString(",", transform = ::getSpigotClassName)
            
            val spigotClassMappings = spigotMappings[mojangClassMappings.name]!!
            return spigotClassMappings.methodMappings[MethodIdentifier(obfuscatedMethodName, spigotReturnTypeClassName, spigotMethodParameters)]
                ?: obfuscatedMethodName
        }
        
        return null
    }
    
    fun resolveFieldLookup(lookup: String, goal: ResolveGoal): String? {
        val lightFieldLookupResult = LIGHT_FIELD_MAPPINGS_LOOKUP_PATTERN.find(lookup)
        if (lightFieldLookupResult != null) {
            val mojangOwnerClassName = lightFieldLookupResult.groups[1]!!.value
            val mojangFieldName = lightFieldLookupResult.groups[2]!!.value
            
            if (goal == ResolveGoal.MOJANG) return mojangFieldName
            
            val mojangClassMappings = mojangMappings[mojangOwnerClassName]!!
            val obfuscatedFieldName = mojangClassMappings.lightFieldMappings[mojangFieldName]!!
            
            if (goal == ResolveGoal.OBFUSCATED || goal == ResolveGoal.SPIGOT) return obfuscatedFieldName
            
            val spigotClassMappings = spigotMappings[mojangClassMappings.name]!!
            return spigotClassMappings.lightFieldMappings[obfuscatedFieldName] ?: obfuscatedFieldName
        }
        
        val fieldLookupResult = FIELD_MAPPINGS_LOOKUP_PATTERN.find(lookup)
        if (fieldLookupResult != null) {
            val mojangOwnerClassName = fieldLookupResult.groups[1]!!.value
            val mojangTypeClassName = fieldLookupResult.groups[2]!!.value
            val mojangFieldName = fieldLookupResult.groups[3]!!.value
            
            if (goal == ResolveGoal.MOJANG) return mojangFieldName
            
            val mojangClassMappings = mojangMappings[mojangOwnerClassName]!!
            val obfuscatedFieldName = mojangClassMappings.fieldMappings[FieldIdentifier(mojangFieldName, mojangTypeClassName)]!!
            
            if (goal == ResolveGoal.OBFUSCATED || goal == ResolveGoal.SPIGOT) return obfuscatedFieldName
            
            val spigotClassMappings = spigotMappings[mojangClassMappings.name]!!
            return spigotClassMappings.lightFieldMappings[obfuscatedFieldName] ?: obfuscatedFieldName
        }
        
        return null
    }
    
    private fun getSpigotClassName(mojangClassName: String): String {
        val obfuscatedName = mojangMappings[mojangClassName]!!.name
        
        val spigotName = spigotMappings[obfuscatedName]?.name
        if (spigotName != null)
            return spigotName
        
        if (obfuscatedName.contains('$')) {
            val names = obfuscatedName.split('$')
            return spigotMappings[names[0]]!!.name + "$" + names[1]
        }
        
        return obfuscatedName
    }
    
    fun processString(value: String, goal: ResolveGoal): String {
        var result = value
        
        generateSequence {
            REMAP_INSTRUCTION_PATTERN.find(result)
        }.forEach { matchResult ->
            val lookupType = matchResult.groups[1]!!.value
            val useSlashes = matchResult.groups[2] != null
            val lookup = matchResult.groups[3]!!.value
            
            var resolvedLookup = when (lookupType) {
                "C" -> resolveClassLookup(lookup, goal)
                "M" -> resolveMethodLookup(lookup, goal)
                "F" -> resolveFieldLookup(lookup, goal)
                else -> throw UnsupportedOperationException()
            } ?: throw IllegalArgumentException("Could not resolve lookup: $lookup")
            
            if (useSlashes)
                resolvedLookup = resolvedLookup.replace('.', '/')
            
            result = result.replaceRange(matchResult.range, resolvedLookup)
        }
        
        return result
    }
    
    enum class ResolveGoal {
        MOJANG,
        OBFUSCATED,
        SPIGOT,
        SPIGOT_MEMBERS
    }
    
}

class ClassMappings(val name: String) {
    
    val fieldMappings = HashMap<FieldIdentifier, String>()
    val lightFieldMappings = HashMap<String, String>()
    val methodMappings = HashMap<MethodIdentifier, String>()
    val lightMethodMappings = HashMap<String, String>()
    
}

data class MethodIdentifier(val name: String, val returnType: String, val parameters: String)

data class FieldIdentifier(val name: String, val type: String)

private fun Reader.useLinesForEach(action: (String) -> Unit) = useLines { it.forEach(action) }