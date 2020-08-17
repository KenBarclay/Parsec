package com.adt.kotlin.parsec.string

import com.adt.kotlin.data.immutable.either.Either
import com.adt.kotlin.parsec.*

import com.adt.kotlin.parsec.CombinatorF.eof
import com.adt.kotlin.parsec.ParsecTF.runP



object StringF {

    val simpleSpace: Parser<Unit> = CombinatorF.skipMany1(CharacterF.satisfy{ch -> Character.isWhitespace(ch)})
    val whiteSpace: Parser<Unit> = CombinatorF.skipMany(simpleSpace)

    fun <A> lexeme(parsec: Parser<A>): Parser<A> =
            parsec.bind{a: A ->
                whiteSpace.bind{_ -> ParsecTF.inject<Char, Unit, A, Unit>(a) }
            }   // lexeme

    fun <A> parse(parsec: Parser<A>, name: String, text: String): Either<ParseError, A> {
        val stream: StringStream = StringStream(text)
        return runP(parsec, Unit, name, stream)
    }   // parse

    fun <A> parseWithEof(parsec: Parser<A>, name: String, text: String): Either<ParseError, A> =
            parse(parsec.sDS(eof()), name, text)

}   // StringF
