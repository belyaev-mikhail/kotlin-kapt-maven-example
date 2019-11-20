package ru.spbstu

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toFileSpec
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutable
import kotlinx.metadata.*
import kotlinx.metadata.jvm.*
import org.intellij.lang.annotations.Language
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



fun kotlin.Metadata.toClassHeader() = KotlinClassHeader(
        kind = kind,
        metadataVersion = metadataVersion,
        bytecodeVersion = bytecodeVersion,
        data1 = data1,
        data2 = data2,
        extraInt = extraInt,
        extraString = extraString,
        packageName = packageName
)

fun kotlin.Metadata.toKMClass() =
        when(val meta = KotlinClassMetadata.read(toClassHeader())) {
            null -> null
            is KotlinClassMetadata.Class -> KmClass().also { meta.accept(it) }
            else -> null
        }

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("ru.spbstu.TestAnnotation")
@SupportedOptions(TestAnnotationProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@AutoService(Processor::class)
class TestAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    @UseExperimental(KotlinPoetMetadataPreview::class)
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val annotatedElements = roundEnv.getElementsAnnotatedWith(TestAnnotation::class.java)
        if (annotatedElements.isEmpty()) return false
        val env = this.processingEnv

        Thread.currentThread().contextClassLoader = javaClass.classLoader

        val kotlinElements =
                roundEnv.getElementsAnnotatedWith(processingEnv.elementUtils.getTypeElement("kotlin.Metadata"))
                        .filter { it in annotatedElements }
        kotlinElements.forEach { println(it.simpleName) }


        val inspector = ElementsClassInspector.create(env.elementUtils, env.typeUtils)
        val classMetadata = kotlinElements.mapNotNull {
            val elem = it.toTypeElementOrNull() ?: return@mapNotNull null
            elem to elem.toTypeSpec(inspector)
        }

        val genFuncs = classMetadata.mapNotNull { (elem, klass) ->
            val poet = klass
            println(poet)
            val primaryFields = poet.primaryConstructor?.parameters.orEmpty()
            println(primaryFields)
            val kname = elem.asClassName()

            val copyFunction = FunSpec
                    .builder("checkedCopy")
                    .receiver(kname)
                    .addParameters(primaryFields.map {
                        ParameterSpec.builder(it.name, it.type).defaultValue("this.${it.name}").build()
                    })
                    .apply {
                        if(primaryFields.isEmpty()) {
                            addCode("return this")
                        } else {
                            val condition = primaryFields.map {
                                CodeBlock.builder().add("(%1N === this.%1N)", it).build()
                            }.joinToCode(" && ")
                            val params = primaryFields.map {
                                CodeBlock.builder().add("%N", it).build()
                            }.joinToCode()
                            addCode("return if(%L) this else %T(%L)", condition, kname, params)
                        }
                    }
                    .build()

            copyFunction
        }

        FileSpec.builder("ru.spbstu", "Generated")
                .apply { genFuncs.forEach { addFunction(it) } }
                .build()
                .writeTo(processingEnv.filer)

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
