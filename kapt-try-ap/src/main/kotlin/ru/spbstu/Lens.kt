package ru.spbstu

import kotlin.reflect.KClass

typealias Lens<S, A> = S.((A) -> A) -> S

interface Lenser<S, A> {
    fun mutate(body: (A) -> A): S

    companion object {
        fun <S> id(obj: S) = Lenser<S, S> { body -> body(obj) }
    }
}

fun <S, A> Lenser<S, A>.set(value: A) = mutate { value }

inline fun <S, A> Lenser(crossinline mutator: ((A) -> A) -> S) = object : Lenser<S, A> {
    override fun mutate(body: (A) -> A): S = mutator(body)
}

fun <S> lens(obj: S) = Lenser.id(obj)

inline operator fun <S, T, A> Lenser<S, T>.plus(crossinline that: Lens<T, A>) = Lenser<S, A> { body ->
    mutate { t -> that(t, body) }
}

@JvmName("eachList")
fun <S, A> Lenser<S, List<A>>.each() = Lenser<S, A> { body ->
    mutate { it.map(body) }
}

@JvmName("eachCollection")
fun <S, A> Lenser<S, Collection<A>>.each() = Lenser<S, A> { body ->
    mutate { it.map(body) }
}

@JvmName("eachIterable")
fun <S, A> Lenser<S, Iterable<A>>.each() = Lenser<S, A> { body ->
    mutate { it.map(body) }
}

@JvmName("eachSequence")
fun <S, A> Lenser<S, Sequence<A>>.each() = Lenser<S, A> { body ->
    mutate { it.map(body) }
}

fun <S, A> Lenser<S, A>.filter(predicate: (A) -> Boolean) = Lenser<S, A> { body ->
    mutate { if(predicate(it)) body(it) else it }
}

fun <S, A: Any> Lenser<S, A?>.notNull() = Lenser<S, A> { body ->
    mutate { if(null === it) it else body(it) }
}

inline fun <S, A: Any, reified B: A> Lenser<S, A>.whenIs(klass: KClass<B> = B::class) = Lenser<S, B> { body ->
    mutate { if(it is B) body(it) else it }
}

/* some examples */

@JvmName("linesString")
fun <S> Lenser<S, String>.lines() = Lenser<S, List<String>> { body ->
    mutate { it.lines().let(body).joinToString("\n") }
}

@JvmName("eachString")
fun <S> Lenser<S, String>.each() = Lenser<S, Char> { body ->
    mutate {
        it.map(body).joinToString("")
    }
}

@JvmName("stringGet")
operator fun <S> Lenser<S, String>.get(index: Int) = Lenser<S, Char> { body ->
    mutate {
        if(it.length <= index) it
        else it.substring(0, index) + body(it[index]) + it.substring(index + 1)
    }
}

fun <S> Lenser<S, String>.substring(startIndex: Int, endIndex: Int) = Lenser<S, String> { body ->
    mutate {
        val pre = it.substring(0, startIndex)
        val post = it.substring(endIndex)
        val mid = body(it.substring(startIndex, endIndex))
        pre + mid + post
    }
}

@get:JvmName("pairFirst")
val <S, A, B> Lenser<S, Pair<A, B>>.first: Lenser<S, A>
    get() = Lenser { body -> mutate { Pair(body(it.first), it.second) } }

@get:JvmName("pairSecond")
val <S, A, B> Lenser<S, Pair<A, B>>.second: Lenser<S, B>
    get() = Lenser { body -> mutate { Pair(it.first, body(it.second)) } }

fun main() {
    val xx = lens("Hello\nworld").lines().each()[0].mutate { 'A' }
    val yy = lens("Hello\nworld").lines().each().substring(0, 2).mutate { it + "+" }
    println(xx)
    println(yy)
}

