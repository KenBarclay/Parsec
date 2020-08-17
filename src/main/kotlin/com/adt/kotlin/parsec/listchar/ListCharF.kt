package com.adt.kotlin.parsec.listchar

import com.adt.kotlin.data.immutable.either.Either
import com.adt.kotlin.data.immutable.list.ListF

import com.adt.kotlin.parsec.ListCharStream
import com.adt.kotlin.parsec.ParseError
import com.adt.kotlin.parsec.ParsecTF
import com.adt.kotlin.parsec.*


object ListCharF {

    val simpleSpace: Parser<Unit> = CombinatorF.skipMany1(CharacterF.satisfy{ch -> Character.isWhitespace(ch)})
    val whiteSpace: Parser<Unit> = CombinatorF.skipMany(simpleSpace)

    fun <A> lexeme(parsec: Parser<A>): Parser<A> =
            parsec.bind{a: A ->
                whiteSpace.bind{_ -> ParsecTF.inject<Char, Unit, A, Unit>(a) }
            }   // lexeme

    fun <A> parse(parser: Parser<A>, name: String, text: String): Either<ParseError, A> {
        val stream: ListCharStream = ListCharStream(ListF.from(text))
        return ParsecTF.runP(parser, Unit, name, stream)
    }   // parse

}   // ListCharF
