package ru.spbstu

import kotlin.reflect.jvm.reflect

@Retention(AnnotationRetention.SOURCE)
@Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.TYPE_PARAMETER,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.FIELD,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.TYPE,
        AnnotationTarget.EXPRESSION,
        AnnotationTarget.FILE,
        AnnotationTarget.TYPEALIAS
)
annotation class OnAnyThing(val x: Int = 5)

@TestAnnotation
data class OhMy(val i: Int)

@TestAnnotation
data class ReallyThere(val i: Double?, @OnAnyThing val s: @OnAnyThing String, val k: OhMy)

@TestAnnotation
object Obj

@TestAnnotation
sealed class Expr

@TestAnnotation
data class Var(val name: String)

@TestAnnotation
data class Const(val value: Int)

@TestAnnotation
class FullBlown {
    @TestAnnotation
    inner class Inner
    @TestAnnotation
    class NotInner

    @TestAnnotation
    companion object {
        @TestAnnotation
        class AlsoAClass
    }
}

@TestAnnotation
enum class EnumExample { BADGER, RED, NINE, NIEN, ROOSEVELT; }

val x = { x: Int -> x + 1 }
public fun code() {
    val x = x.reflect()
    x?.call(x)
}
