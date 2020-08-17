package com.adt.kotlin.parsec.language

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.list.ListF
import com.adt.kotlin.parsec.CharacterF.alphaNum
import com.adt.kotlin.parsec.CharacterF.letter
import com.adt.kotlin.parsec.CharacterF.oneOf

object KotlinLanguageDef : LanguageDef {

    override val commentStart: String = "/*"
    override val commentEnd: String = "*/"
    override val commentLine: String = "//"
    override val nestedComments: Boolean = true

    override val identStart: Parsec<Char> = letter
    override val identLetter: Parsec<Char> = alphaNum

    override val opStart: Parsec<Char> = oneOf(ListF.of('+', '-', '*', '/', '%'))
    override val opLetter: Parsec<Char> = oneOf(ListF.of())

    override val reservedNames: List<String> = ListF.of()
    override val reservedOpNames: List<String> = ListF.of()
    override val caseSensitive: Boolean = true

}   // KotlinLanguageDef
