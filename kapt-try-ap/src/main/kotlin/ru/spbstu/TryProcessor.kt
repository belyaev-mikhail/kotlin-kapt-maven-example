package ru.spbstu

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TestAnnotation

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
            val elem = it as? TypeElement ?: return@mapNotNull null
            elem to elem.toTypeSpec(inspector)
        }

        val genFuncs = classMetadata.flatMap { (elem, klass) ->
            val poet = klass
            val primaryFields = poet.primaryConstructor?.parameters.orEmpty()
            val kname = elem.asClassName()

            val lenser = ClassName("ru.spbstu", "Lenser")
            val s = TypeVariableName("S")
            val escapedTyVars = poet.typeVariables.map {
                TypeVariableName.invoke("Bu" + it.name, bounds = it.bounds)
            }
            val tyVarMapping = poet.typeVariables.zip(escapedTyVars).toMap()

            primaryFields.map { field ->
                val escapedFieldType = field.type.subst(tyVarMapping)
                val jvmName = elem.simpleName.toString().decapitalize() + field.name.capitalize()
                PropertySpec
                        .builder(field.name, lenser.parameterizedBy(s, escapedFieldType))
                        .addTypeVariable(s)
                        .addTypeVariables(escapedTyVars)
                        .receiver(lenser.parameterizedBy(s, kname.plusParameters(escapedTyVars)))
                        .addAnnotation(
                                AnnotationSpec
                                        .builder(JvmName::class)
                                        .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                                        .addMember("%S", jvmName)
                                        .build()
                        )
                        .getter {
                            addCode(
                                    """
                                    return %T { body ->
                                        mutate {
                                            %T(%L)
                                        }
                                    }
                                    """.trimIndent(),
                                    lenser.parameterizedBy(s, escapedFieldType),
                                    kname,
                                    primaryFields.map {
                                        if (it.name == field.name) {
                                            CodeBlock.of("%1N = body(it.%1N)", it)
                                        } else {
                                            CodeBlock.of("%1N = it.%1N", it)
                                        }
                                    }.joinToCode(", ")
                            )
                        }
                        .build()
            }
        }

        FileSpec.builder("ru.spbstu", "Generated")
                .apply { genFuncs.forEach { addProperty(it) } }
                .build()
                .writeTo(processingEnv.filer)

        return true
    }

}
