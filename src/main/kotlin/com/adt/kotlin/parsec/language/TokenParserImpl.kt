package com.adt.kotlin.parsec.language

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.list.List.Nil
import com.adt.kotlin.data.immutable.list.List.Cons
import com.adt.kotlin.data.immutable.list.ListF
import com.adt.kotlin.data.immutable.list.charsToString
import com.adt.kotlin.parsec.*

import com.adt.kotlin.parsec.CombinatorF.between
import com.adt.kotlin.parsec.CombinatorF.choice
import com.adt.kotlin.parsec.CombinatorF.many
import com.adt.kotlin.parsec.CombinatorF.many1
import com.adt.kotlin.parsec.CombinatorF.notFollowedBy
import com.adt.kotlin.parsec.CombinatorF.sepBy
import com.adt.kotlin.parsec.CombinatorF.sepBy1
import com.adt.kotlin.parsec.CombinatorF.skipMany
import com.adt.kotlin.parsec.CombinatorF.skipMany1
import com.adt.kotlin.parsec.ParsecTF.inject
import com.adt.kotlin.parsec.ParsecTF.unexpected
import com.adt.kotlin.parsec.CharacterF.character
import com.adt.kotlin.parsec.CharacterF.digit
import com.adt.kotlin.parsec.CharacterF.hexDigit
import com.adt.kotlin.parsec.CharacterF.octDigit
import com.adt.kotlin.parsec.CharacterF.oneOf
import com.adt.kotlin.parsec.CharacterF.satisfy
import com.adt.kotlin.parsec.CharacterF.string



open class TokenParserImpl(val languageDef: LanguageDef) {

    fun makeTokenParser(): TokenParser =
            object: TokenParser {
                override val identifier: Parsec<String> = identifierX
                override fun reserved(str: String): Parsec<Unit> = reservedX(str)
                override val operator: Parsec<String> = operatorX
                override fun reservedOp(str: String): Parsec<Unit> = reservedOpX(str)
                override val charLiteral: Parsec<Char> = charLiteralX
                override val stringLiteral: Parsec<String> = stringLiteralX
                override val integer: Parsec<Int> = integerX
                override val hexadecimal: Parsec<Int> = hexadecimalX
                override val octal: Parsec<Int> = octalX
                ////val floatingPoint: Parsec<Double>

                override fun <A> lexeme(parsec: Parsec<A>): Parsec<A> = lexemeX(parsec)

                override val whiteSpace: Parsec<Unit> = whiteSpaceX

                override fun <A> parens(parsec: Parsec<A>): Parsec<A> = parensX(parsec)
                override fun <A> braces(parsec: Parsec<A>): Parsec<A> = bracesX(parsec)
                override fun <A> angles(parsec: Parsec<A>): Parsec<A> = anglesX(parsec)
                override fun <A> brackets(parsec: Parsec<A>): Parsec<A> = bracketsX(parsec)

                override val semi: Parsec<String> = semiX
                override val comma: Parsec<String> = commaX
                override val colon: Parsec<String> = colonX
                override val dot: Parsec<String> = dotX

                override fun <A> semiSep(parsec: Parsec<A>): Parsec<List<A>> = semiSepX(parsec)
                override fun <A> semiSep1(parsec: Parsec<A>): Parsec<List<A>> = semiSep1X(parsec)

                override fun <A> commaSep(parsec: Parsec<A>): Parsec<List<A>> = commaSepX(parsec)
                override fun <A> commaSep1(parsec: Parsec<A>): Parsec<List<A>> = commaSep1X(parsec)
            }   // makeTokenParser



// ---------- implementation ------------------------------

    // Bracketing

    private fun <A> parensX(parsec: Parsec<A>) = between(symbol("("), symbol(")"), parsec)
    private fun <A> bracesX(parsec: Parsec<A>) = between(symbol("("), symbol(")"), parsec)
    private fun <A> anglesX(parsec: Parsec<A>) = between(symbol("("), symbol(")"), parsec)
    private fun <A> bracketsX(parsec: Parsec<A>) = between(symbol("("), symbol(")"), parsec)

    private val semiX: Parsec<String> = symbol(";")
    private val commaX: Parsec<String> = symbol(",")
    private val dotX: Parsec<String> = symbol(".")
    private val colonX: Parsec<String> = symbol(":")

    private fun <A> commaSepX(parsec: ParsecT<Char, Unit, A, Unit>): ParsecT<Char, Unit, List<A>, Unit> =
            sepBy(parsec, commaX)
    private fun <A> semiSepX(parsec: ParsecT<Char, Unit, A, Unit>): ParsecT<Char, Unit, List<A>, Unit> =
            sepBy(parsec, semiX)

    private fun <A> commaSep1X(parsec: ParsecT<Char, Unit, A, Unit>): ParsecT<Char, Unit, List<A>, Unit> =
            sepBy1(parsec, commaX)
    private fun <A> semiSep1X(parsec: ParsecT<Char, Unit, A, Unit>): ParsecT<Char, Unit, List<A>, Unit> =
            sepBy1(parsec, semiX)



    // Characters and strings

    private val escChar: List<Char> = ListF.of('a',      'b',  'f',      'n',  'r',  't',  'v',      '\\', '\"', '\'')
    private val escCode: List<Char> = ListF.of('\u0006', '\b', '\u000C', '\n', '\r', '\t', '\u000B', '\\', '\"', '\'')
    private val escMap: List<Pair<Char, Char>> = escChar.zip(escCode)

    private val charLetter: Parsec<Char> = satisfy{ch: Char -> (ch != '\'') && (ch != '\\') && (ch > '\u0020')}
    private val charCharacter: Parsec<Char> = charLetter    // TODO
    private val charLiteralX: Parsec<Char> =
            lexemeX(
                    between(character('\''), character('\'').label("end of character"), charCharacter)
            )

    private val charEscape: Parsec<Char> = character('\\').bind{_: Char -> escapeCode}
    private val escapeCode: Parsec<Char> = charEsc    // TODO TODO
    private val charEsc: Parsec<Char>
        get() {
            fun parEsc(ch: Char, code: Char): Parsec<Char> = character(ch).bind{_: Char -> inject<Char, Unit, Char, Unit>(code)}
            return choice(escMap.map{pr -> parEsc(pr.first, pr.second)})
        }

    private val stringLetter: Parsec<Char> = satisfy{ch: Char -> (ch != '\'') && (ch != '\\') && (ch > '\u0020')}
    private val stringChar: Parsec<Char> = stringLetter // TODO
    private val stringLiteralX: Parsec<String> =
            lexemeX(
                    between(character('"'), character('"').label("end of string"), many(stringChar)).bind{str: List<Char> ->
                        inject<Char, Unit, String, Unit>(str.charsToString())
                    }
            )



    // Numbers

    fun number(base: Int, digit: Parsec<Char>): Parsec<Int> =
            many1(digit).bind{digs: List<Char> ->
                val n: Int = digs.foldLeft(0){acc: Int -> {ch: Char -> base * acc + (ch.toInt() - '0'.toInt())}}
                inject<Char, Unit, Int, Unit>(n)
            }
    val zeroNumber: Parsec<Int> =
            character('0').bind{_: Char ->
                hexadecimalX.choice(octalX).choice(decimal).choice(inject(0))
            }
    val decimal: Parsec<Int> = number(10, digit)
    val hexadecimalX: Parsec<Int> =
            oneOf(ListF.of('x', 'X')).bind{_: Char ->
                number(16, hexDigit)
            }
    val octalX: Parsec<Int> =
            oneOf(ListF.of('0')).bind{_: Char ->
                number(8, octDigit)
            }
    val nat: Parsec<Int> = zeroNumber.choice(decimal)
    val intX: Parsec<Int> = nat
    val integerX: Parsec<Int> = lexemeX(intX).label("integer")


    // White space and symbols

    fun symbol(text: String): Parsec<String> =
            lexemeX(string(text))

    val simpleSpace: Parsec<Unit> = skipMany1(satisfy{ch -> Character.isWhitespace(ch)})
    val whiteSpaceX: Parsec<Unit> = skipMany(simpleSpace) // TODO TODO

    fun <A> lexemeX(parsec: Parsec<A>): Parsec<A> =
            parsec.bind{a: A ->
                whiteSpaceX.bind{_ -> inject<Char, Unit, A, Unit>(a)}
            }   // lexeme



    // Identifiers and reserved words

    private val ident: Parsec<String> =
            languageDef.identStart.bind{ch: Char ->
                many(languageDef.identLetter).bind{chs: List<Char> ->
                    inject<Char, Unit, String, Unit>(ListF.cons(ch, chs).charsToString())
                }
            }

    private val identifierX: Parsec<String> =
            lexemeX(
                ident.bind{name ->
                    if (isReservedName(name)) unexpected("reserved word: $name") else inject<Char, Unit, String, Unit>(name)
                }.`try`()
            )

    private fun reservedX(str: String): Parsec<Unit> =
            lexemeX(
                    caseString(str).bind{_: String ->
                        notFollowedBy(languageDef.identLetter)
                    }.`try`()
            )

    private fun caseString(str: String): Parsec<String> {
        fun caseChar(ch: Char): Parsec<Char> =
                if (Character.isLetterOrDigit(ch))
                    character(Character.toLowerCase(ch)).choice(character(Character.toUpperCase(ch)))
                else
                    character(ch)
        fun walk(chs: List<Char>): Parsec<Unit> =
                when (chs) {
                    is Nil -> inject(Unit)
                    is Cons -> caseChar(chs.head()).bind{_: Char ->
                        walk(chs.tail())
                    }
                }

        return if (languageDef.caseSensitive)
            string(str)
        else
            walk(ListF.from(str)).bind{_: Unit -> inject<Char, Unit, String, Unit>(str)}
    }



    // Operators and reserved operators

    private val oper: Parsec<String> =
            languageDef.opStart.bind{ch: Char ->
                many(languageDef.opLetter).bind{chs: List<Char> ->
                    inject<Char, Unit, String, Unit>(ListF.cons(ch, chs).charsToString())
                }
            }

    private val operatorX: Parsec<String> =
            lexemeX(
                    oper.bind{name ->
                        if (isReservedOp(name)) unexpected("reserved operator: $name") else inject<Char, Unit, String, Unit>(name)
                    }.`try`()
            )

    private fun reservedOpX(str: String): Parsec<Unit> =
            lexemeX(
                    string(str).bind{_: String ->
                        notFollowedBy(languageDef.opLetter)
                    }
            )



    // Escape code tables



    // XXX

    private fun isReservedName(name: String): Boolean =
            isReserved(reservedNames, if (languageDef.caseSensitive) name else name.toLowerCase())

    private fun isReserved(names: List<String>, name: String): Boolean = names.contains(name)

    private val reservedNames: List<String> =
            if (languageDef.caseSensitive) languageDef.reservedNames else languageDef.reservedNames.map{name -> name.toLowerCase()}

    private fun isReservedOp(name: String): Boolean =
            isReserved(languageDef.reservedOpNames, name)

}   // TokenParserF
