package com.adt.kotlin.parsec.language

import com.adt.kotlin.data.immutable.list.List

import com.adt.kotlin.parsec.ParsecT



typealias Parsec<A> = ParsecT<Char, Unit, A, Unit>

interface LanguageDef {

    val commentStart: String
    val commentEnd: String
    val commentLine: String
    val nestedComments: Boolean

    val identStart: Parsec<Char>
    val identLetter: Parsec<Char>

    val opStart: Parsec<Char>
    val opLetter: Parsec<Char>

    val reservedNames: List<String>
    val reservedOpNames: List<String>
    val caseSensitive: Boolean

}   // LanguageDef
