import com.google.auto.service.AutoService
import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.KotlinClassMetadata.Companion.read
import java.io.File
import java.net.URLClassLoader
import java.util.*
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

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("TestAnnotation")
@SupportedOptions(TestAnnotationProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@AutoService(Processor::class)
class TestAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val annotatedElements = roundEnv.getElementsAnnotatedWith(TestAnnotation::class.java)
        if (annotatedElements.isEmpty()) return false

        Thread.currentThread().contextClassLoader = javaClass.classLoader

        val kotlinElements = annotatedElements.filter { it.annotationMirrors.any { it.isKotlinMetadata() } }

        val classMetadata = kotlinElements.mapNotNull { it.kotlinClass()?.let { KotlinClassMetadata.read(it) } }

        classMetadata.forEach { it as KotlinClassMetadata.Class; it.accept(object : KmClassVisitor() {
            override fun visit(flags: Int, name: ClassName) {
                println("class $name")
            }

            override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? {

                val des = if(Flag.Property.IS_VAR(flags)) "var" else "val"

                print("$des $name")

                return object : KmPropertyVisitor() {

                    override fun visitReturnType(flags: Flags): KmTypeVisitor? {
                        return object : KmTypeVisitor() {
                            override fun visitClass(name: ClassName) {
                                println(": $name")
                            }
                        }
                    }
                }
            }
        }) }

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
//            writeText(generatedKtFile.accept(PrettyPrinter(PrettyPrinterConfiguration())))
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