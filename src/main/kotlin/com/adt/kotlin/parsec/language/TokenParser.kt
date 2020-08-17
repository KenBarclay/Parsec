package com.adt.kotlin.parsec.language

import com.adt.kotlin.data.immutable.list.List

import com.adt.kotlin.parsec.ParsecT



interface TokenParser {

    val identifier: Parsec<String>

    fun reserved(str: String): Parsec<Unit>

    val operator: Parsec<String>

    fun reservedOp(str: String): Parsec<Unit>

    val charLiteral: Parsec<Char>
    val stringLiteral: Parsec<String>

    val integer: Parsec<Int>
    val hexadecimal: Parsec<Int>
    val octal: Parsec<Int>
    ////val floatingPoint: Parsec<Double>

    fun <A> lexeme(parsec: Parsec<A>): Parsec<A>

    val whiteSpace: Parsec<Unit>

    fun <A> parens(parsec: Parsec<A>): Parsec<A>
    fun <A> braces(parsec: Parsec<A>): Parsec<A>
    fun <A> angles(parsec: Parsec<A>): Parsec<A>
    fun <A> brackets(parsec: Parsec<A>): Parsec<A>

    val semi: Parsec<String>
    val comma: Parsec<String>
    val colon: Parsec<String>
    val dot: Parsec<String>

    fun <A> semiSep(parsec: Parsec<A>): Parsec<List<A>>
    fun <A> semiSep1(parsec: Parsec<A>): Parsec<List<A>>

    fun <A> commaSep(parsec: Parsec<A>): Parsec<List<A>>
    fun <A> commaSep1(parsec: Parsec<A>): Parsec<List<A>>

}   // TokenParser
