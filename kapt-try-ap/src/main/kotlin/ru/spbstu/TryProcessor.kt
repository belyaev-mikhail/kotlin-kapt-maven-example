package ru.spbstu

import com.google.auto.service.AutoService
import kotlinx.metadata.*
import kotlinx.metadata.jvm.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

import javax.tools.Diagnostic.Kind.ERROR

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TestAnnotation

fun AnnotationMirror.isKotlinMetadata() =
        (annotationType.asElement() as TypeElement).qualifiedName.contentEquals("kotlin.Metadata")

fun AnnotationValue?.asInt() = this?.value as? Int
fun AnnotationValue?.asString() = this?.value as? String
fun AnnotationValue?.asArray() = this?.value as? List<AnnotationValue>

fun List<Int>.toIntArray() = IntArray(size) { get(it) }

fun Element.kotlinClass() =
        annotationMirrors.firstOrNull() { it.isKotlinMetadata() }?.let {
            val elems = it.elementValues.map { it.key.simpleName.toString() to it.value }.toMap()

            val k = elems["k"].asInt()
            val mv = elems["mv"].asArray()?.mapNotNull { it.asInt() }?.toIntArray()
            val bv = elems["bv"].asArray()?.mapNotNull { it.asInt() }?.toIntArray()
            val d1 = elems["d1"].asArray()?.mapNotNull { it.asString() }?.toTypedArray()
            val d2 = elems["d2"].asArray()?.mapNotNull { it.asString() }?.toTypedArray()
            val xs = elems["xs"].asString()
            val pn = elems["pn"].asString()
            val xi = elems["xi"].asInt()

            KotlinClassHeader(k, mv, bv, d1, d2, xs, pn, xi)
        }

interface ExplorationVisitor {
    var indent: Int
    fun println(v: String)

    companion object {
        class It(override var indent: Int = 0): ExplorationVisitor {
            operator fun String.times(pow: Int) = repeat(pow)
            override fun println(v: String) = kotlin.io.println(v.prependIndent("    " * indent))
        }

        operator fun invoke(indent: Int = 0) = It(indent)
    }
}

class ExplorationKmTypeParameterVisitor(indent: Int = 0) : KmTypeParameterVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitEnd() {
        println("visitEnd()")
        super.visitEnd()
    }

    override fun visitExtensions(type: KmExtensionType): KmTypeParameterExtensionVisitor? {
        println("visitExtensions(type = ${type})")
        if(type != JvmTypeParameterExtensionVisitor.TYPE) return null
        return JvmExtension()
    }

    override fun visitUpperBound(flags: Flags): KmTypeVisitor? {
        println("visitUpperBound(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    inner class JvmExtension : JvmTypeParameterExtensionVisitor() {
        override fun visitAnnotation(annotation: KmAnnotation) {
            println("visitAnnotation(annotation = ${annotation})")
            super.visitAnnotation(annotation)
        }

        override fun visitEnd() {
            println("visitEnd()")
            super.visitEnd()
        }
    }
}

class ExplorationKmValueParameterVisitor(indent: Int = 0) : KmValueParameterVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitEnd() {
        println("visitEnd()")
        super.visitEnd()
    }

    override fun visitType(flags: Flags): KmTypeVisitor? {
        println("visitType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitVarargElementType(flags: Flags): KmTypeVisitor? {
        println("visitVarargElementType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }
}

class ExplorationKmTypeAliasVisitor(indent: Int = 0) : KmTypeAliasVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitAnnotation(annotation: KmAnnotation) {
        println("visitAnnotation(annotation = ${annotation})")
        super.visitAnnotation(annotation)
    }

    override fun visitEnd() {
        println("visitEnd()")
        super.visitEnd()
    }

    override fun visitExpandedType(flags: Flags): KmTypeVisitor? {
        println("visitExpandedType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? {
        println("visitTypeParameter(flags = ${flags}, name = ${name}, id = ${id}, variance = ${variance})")
        return ExplorationKmTypeParameterVisitor(indent)
    }

    override fun visitUnderlyingType(flags: Flags): KmTypeVisitor? {
        println("visitUnderlyingType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? {
        println("visitVersionRequirement()")
        return super.visitVersionRequirement()
    }
}

class ExplorationKmTypeVisitor(indent: Int = 0) : KmTypeVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitAbbreviatedType(flags: Flags): KmTypeVisitor? {
        println("visitAbbreviatedType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor? {
        println("visitArgument(flags = ${flags}, variance = ${variance})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitClass(name: ClassName) {
        println("visitClass(name = ${name})")
        println("jvmName = ${name.jvmInternalName}")
        super.visitClass(name)
    }

    override fun visitEnd() {
        println("visitEnd()")
        super.visitEnd()
    }

    override fun visitExtensions(type: KmExtensionType): KmTypeExtensionVisitor? {
        println("visitExtensions(type = ${type})")
        return if(type == JvmTypeExtensionVisitor.TYPE) JvmExtension() else null
    }

    inner class JvmExtension : JvmTypeExtensionVisitor() {
        override fun visit(isRaw: Boolean) {
            println("visit(isRaw = ${isRaw})")
        }

        override fun visitAnnotation(annotation: KmAnnotation) {
            println("visitAnnotation(annotation = ${annotation})")
        }

        override fun visitEnd() {
            println("visitEnd()")
        }
    }

    override fun visitFlexibleTypeUpperBound(flags: Flags, typeFlexibilityId: String?): KmTypeVisitor? {
        println("visitFlexibleTypeUpperBound(flags = ${flags}, typeFlexibilityId = ${typeFlexibilityId})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitOuterType(flags: Flags): KmTypeVisitor? {
        println("visitOuterType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitStarProjection() {
        println("visitStarProjection()")
        super.visitStarProjection()
    }

    override fun visitTypeAlias(name: ClassName) {
        println("visitTypeAlias(name = ${name})")
        super.visitTypeAlias(name)
    }

    override fun visitTypeParameter(id: Int) {
        println("visitTypeParameter(id = ${id})")
        super.visitTypeParameter(id)
    }
}

class ExplorationKmPropertyVisitor(indent: Int = 0) : KmPropertyVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitEnd() {
        println("visitEnd()")
    }

    override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? {
        println("visitExtensions(type = ${type})")
        return if(type == JvmPropertyExtensionVisitor.TYPE) JvmExtensions() else null
    }

    inner class JvmExtensions : JvmPropertyExtensionVisitor() {
        override fun visit(fieldName: String?, fieldTypeDesc: String?, getterDesc: String?, setterDesc: String?) {
            println("visit(fieldName = ${fieldName}, fieldTypeDesc = ${fieldTypeDesc}, getterDesc = ${getterDesc}, setterDesc = ${setterDesc})")
        }

        override fun visitEnd() {
            println("visitEnd()")
        }

        override fun visitSyntheticMethodForAnnotations(desc: String?) {
            println("visitSyntheticMethodForAnnotations(desc = ${desc})")
        }
    }

    override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? {
        println("visitReceiverParameterType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitReturnType(flags: Flags): KmTypeVisitor? {
        println("visitReturnType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitSetterParameter(flags: Flags, name: String): KmValueParameterVisitor? {
        println("visitSetterParameter(flags = ${flags}, name = ${name})")
        return ExplorationKmValueParameterVisitor(indent)
    }

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? {
        println("visitTypeParameter(flags = ${flags}, name = ${name}, id = ${id}, variance = ${variance})")
        return ExplorationKmTypeParameterVisitor(indent)
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? {
        println("visitVersionRequirement()")
        return super.visitVersionRequirement()
    }
}

class ExplorationKmFunctionVisitor(indent: Int = 0) : KmFunctionVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitContract(): KmContractVisitor? {
        println("visitContract()")
        return super.visitContract()
    }

    override fun visitEnd() {
        println("visitEnd()")
        super.visitEnd()
    }

    override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
        println("visitExtensions(type = ${type})")
        return if(type == JvmFunctionExtensionVisitor.TYPE) JvmExtension() else null
    }

    inner class JvmExtension : JvmFunctionExtensionVisitor() {
        override fun visit(desc: String?) {
            println("visit(desc = ${desc})")
        }

        override fun visitLambdaClassOriginName(internalName: String) {
            println("visitLambdaClassOriginName(internalName = ${internalName})")
        }
    }

    override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? {
        println("visitReceiverParameterType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitReturnType(flags: Flags): KmTypeVisitor? {
        println("visitReturnType(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? {
        println("visitTypeParameter(flags = ${flags}, name = ${name}, id = ${id}, variance = ${variance})")
        return ExplorationKmTypeParameterVisitor(indent)
    }

    override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? {
        println("visitValueParameter(flags = ${flags}, name = ${name})")
        return ExplorationKmValueParameterVisitor(indent)
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? {
        println("visitVersionRequirement()")
        return super.visitVersionRequirement()
    }
}

class ExplorationKmClassConstructorVisitor(indent: Int = 0) : KmConstructorVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitEnd() {
        --indent
        println("visitEnd()")
    }

    override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? {
        println("visitExtensions(type = ${type})")
        return if(type == JvmConstructorExtensionVisitor.TYPE) JvmExtension() else null
    }

    inner class JvmExtension : JvmConstructorExtensionVisitor() {
        override fun visit(desc: String?) {
            println("visit(desc = ${desc})")
        }
    }

    override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? {
        println("visitValueParameter(flags = ${flags}, name = ${name})")
        return ExplorationKmValueParameterVisitor(indent)
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? {
        println("visitVersionRequirement()")
        return super.visitVersionRequirement()
    }
}

class ExplorationKmClassVisitor(indent: Int = 0) : KmClassVisitor(), ExplorationVisitor by ExplorationVisitor(indent) {
    override fun visit(flags: Flags, name: ClassName) {
        println("visit(flags = ${flags}, name = ${name})")
        println("jvmInternalName = ${name.jvmInternalName}")
        ++indent
    }
    override fun visitCompanionObject(name: String) = println("visitCompanionObject(name = ${name})")
    override fun visitConstructor(flags: Flags): KmConstructorVisitor? {
        println("visitConstructor(flags = ${flags})")
        return ExplorationKmClassConstructorVisitor(indent)
    }

    override fun visitEnd() {
        --indent
        println("visitEnd()")
    }

    override fun visitEnumEntry(name: String) = println("visitEnumEntry(name = ${name})")

    override fun visitExtensions(type: KmExtensionType): KmClassExtensionVisitor? {
        println("visitExtensions(type = ${type})")
        return if(type == JvmClassExtensionVisitor.TYPE) JvmExtensions() else null
    }
    inner class JvmExtensions : JvmClassExtensionVisitor() {
        override fun visitAnonymousObjectOriginName(internalName: String) {
            println("visitAnonymousObjectOriginName(internalName = ${internalName})")
        }
    }

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        println("visitFunction(flags = ${flags}, name = ${name})")
        return ExplorationKmFunctionVisitor(indent)
    }

    override fun visitNestedClass(name: String) = println("visitNestedClass(name = ${name})")

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? {
        println("visitProperty(flags = ${flags}, name = ${name}, getterFlags = ${getterFlags}, setterFlags = ${setterFlags})")
        return ExplorationKmPropertyVisitor(indent)
    }

    override fun visitSealedSubclass(name: ClassName) {
        println("visitSealedSubclass(name = ${name})")
        super.visitSealedSubclass(name)
    }

    override fun visitSupertype(flags: Flags): KmTypeVisitor? {
        println("visitSupertype(flags = ${flags})")
        return ExplorationKmTypeVisitor(indent)
    }

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? {
        println("visitTypeAlias(flags = ${flags}, name = ${name})")
        return ExplorationKmTypeAliasVisitor(indent)
    }

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? {
        println("visitTypeParameter(flags = ${flags}, name = ${name}, id = ${id}, variance = ${variance})")
        return ExplorationKmTypeParameterVisitor(indent)
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? {
        println("visitVersionRequirement()")
        return super.visitVersionRequirement()
    }
}

class ExplorationKmPackageVisitor(indent: Int = 0) : KmPackageVisitor(), ExplorationVisitor by ExplorationVisitor(indent) {
    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        println("visitFunction(flags = ${flags}, name = ${name})")
        return ExplorationKmFunctionVisitor(indent)
    }

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? {
        println("visitProperty(flags = ${flags}, name = ${name}, getterFlags = ${getterFlags}, setterFlags = ${setterFlags})")
        return ExplorationKmPropertyVisitor(indent)
    }

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? {
        println("visitTypeAlias(flags = ${flags}, name = ${name})")
        return ExplorationKmTypeAliasVisitor(indent)
    }
}


class ExplorationKmLambdaVisitor(indent: Int = 0) : KmLambdaVisitor(), ExplorationVisitor by ExplorationVisitor(indent + 1) {
    override fun visitEnd() {
        println("visitEnd()")
        super.visitEnd()
    }

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        println("visitFunction(flags = ${flags}, name = ${name})")
        return ExplorationKmFunctionVisitor(indent)
    }
}

class ExplorationKmTopLevelVisitor(indent: Int = 0) : ExplorationVisitor by ExplorationVisitor(indent + 1) {
    fun visitClass(): KmClassVisitor {
        println("visitClass()")
        return ExplorationKmClassVisitor(indent)
    }
    fun visitFileFacade(): KmPackageVisitor {
        println("visitFileFacade()")
        return ExplorationKmPackageVisitor(indent)
    }
    fun visitSyntheticClass(isLambda: Boolean): KmLambdaVisitor {
        println("visitSyntheticClass(isLambda = ${isLambda})")
        return ExplorationKmLambdaVisitor(indent)
    }
    fun visitMultiFileClassFacade(partClassNames: List<String>) {
        println("visitMultiFileClassFacade(partClassNames = ${partClassNames})")
    }
    fun visitMultiFileClassPart(): KmPackageVisitor {
        println("visitMultiFileClassPart()")
        return ExplorationKmPackageVisitor(indent)
    }
    fun visitUnknown() {
        println("visitUnknown()")
    }
    fun visitEnd() {
        println("visitEnd()")
    }
}

fun KotlinClassMetadata.accept(visitor: ExplorationKmTopLevelVisitor): Any {
    val x: Any = when(this) {
        is KotlinClassMetadata.Class -> accept(visitor.visitClass())
        is KotlinClassMetadata.FileFacade -> accept(visitor.visitFileFacade())
        is KotlinClassMetadata.SyntheticClass -> visitor.visitSyntheticClass(isLambda)
        is KotlinClassMetadata.MultiFileClassFacade -> visitor.visitMultiFileClassFacade(partClassNames)
        is KotlinClassMetadata.MultiFileClassPart -> accept(visitor.visitMultiFileClassPart())
        is KotlinClassMetadata.Unknown -> visitor.visitUnknown()
    }
    visitor.visitEnd()
    return 0
}

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("ru.spbstu.TestAnnotation")
@SupportedOptions(TestAnnotationProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@AutoService(Processor::class)
class TestAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val annotatedElements = roundEnv.getElementsAnnotatedWith(TestAnnotation::class.java)
        if (annotatedElements.isEmpty()) return false

        println(roundEnv.rootElements.mapNotNull { it.kotlinClass() })

        Thread.currentThread().contextClassLoader = javaClass.classLoader

        val kotlinElements = roundEnv.getElementsAnnotatedWith(processingEnv.elementUtils.getTypeElement("kotlin.Metadata"))
        kotlinElements.forEach { println(it.simpleName) }

        val classMetadata = kotlinElements.mapNotNull { it.kotlinClass()?.let { KotlinClassMetadata.read(it) } }

        classMetadata.asSequence()
                .forEach {
                    it.accept(ExplorationKmTopLevelVisitor(-1))
                }

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
            processingEnv.messager.printMessage(ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }

        File(kaptKotlinGeneratedDir, "Generated.kt")
                .apply { parentFile.mkdirs() }
                .apply { createNewFile() }
                .writeText(/*language=kotlin*/"""
                    fun hello() = "Hello world"
                """.trimIndent())

//        val generatedKtFile = kotlinFile("test.generated") {
//            for (element in annotatedElements) {
//                val typeElement = element.toTypeElementOrNull() ?: continue
//
//                property("simpleClassName") {
//                    receiverType(typeElement.qualifiedName.toString())
//                    getterExpression("this::class.java.simpleName")
//                }
//            }
//        }
//
//        File(kaptKotlinGeneratedDir, "testGenerated.kt").apply {
//            parentFile.mkdirs()
//            writeText(generatedKtFile.ru.spbstu.accept(PrettyPrinter(PrettyPrinterConfiguration())))
//        }

        return true
    }

    fun Element.toTypeElementOrNull(): TypeElement? {
        if (this !is TypeElement) {
            processingEnv.messager.printMessage(ERROR, "Invalid element type, class expected", this)
            return null
        }

        return this
    }
}
