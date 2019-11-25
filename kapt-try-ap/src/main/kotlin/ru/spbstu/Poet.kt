package ru.spbstu

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun PropertySpec.Builder.getter(body: FunSpec.Builder.() -> Unit) =
        getter(FunSpec.getterBuilder().apply(body).build())
fun ClassName.plusParameters(parameters: List<TypeName>) =
        if(parameters.isEmpty()) this else parameterizedBy(parameters)
fun FunSpec.Builder.addCode(body: CodeBlock.Builder.() -> Unit): FunSpec.Builder =
        addCode(CodeBlock.builder().apply(body).build())

fun TypeName.subst(mapping: Map<TypeVariableName, TypeName>): TypeName = when(this) {
    is ClassName, is LambdaTypeName, is WildcardTypeName, Dynamic -> this
    is TypeVariableName -> mapping[this] ?: this
    is ParameterizedTypeName ->
        rawType.parameterizedBy(this.typeArguments.map { it.subst(mapping) })
                .copy(nullable = isNullable, annotations = annotations)
}
