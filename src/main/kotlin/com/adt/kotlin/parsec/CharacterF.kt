package com.adt.kotlin.parsec

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.list.ListF
import com.adt.kotlin.data.immutable.list.charsToString

import com.adt.kotlin.data.immutable.option.OptionF.none
import com.adt.kotlin.data.immutable.option.OptionF.some

import com.adt.kotlin.hkfp.fp.FunctionF.C2

import com.adt.kotlin.parsec.ParsecTF.tokenPrim
import com.adt.kotlin.parsec.ParsecTF.tokens
import com.adt.kotlin.parsec.PositionF.updateChar
import com.adt.kotlin.parsec.PositionF.updateString
import com.adt.kotlin.parsec.CombinatorF.skipMany



typealias Parser<A> = ParsecT<Char, Unit, A, Unit>

/**
 * Commonly used character parsers.
 */
object CharacterF {

    /**
     * The parser satisfy(predicate) succeeds for any character for which
     *   the predicate returns true. Returns the character that is parsed.
     */
    fun satisfy(predicate: (Char) -> Boolean): Parser<Char> =
            tokenPrim(
                    {pos: SourcePosition -> {tok: Char -> {_: Stream<Char> -> updateChar(pos, tok)}}},
                    {c: Char -> if (predicate(c)) some(c) else none()}
            )   // satisfy

    /**
     * This parser succeeds for any character.
     */
    val anyCharacter: Parser<Char>
        get() = satisfy{_: Char -> true}

    /**
     * The parser character(ch) parses the single character ch. Returns the
     *   parsed character.
     */
    fun character(ch: Char): Parser<Char> =
            satisfy{c: Char -> (c == ch)}.label("$ch")

    /**
     * The parser oneOf(chars) succeeds if the current input character is
     *   in the list of chars. Returns the parsed character.
     */
    fun oneOf(chars: List<Char>): Parser<Char> =
            satisfy{ch: Char -> chars.contains(ch)}

    fun oneOf(vararg chars: Char): Parser<Char> =
            satisfy{ch: Char -> chars.contains(ch)}

    /**
     * The parser noneOf(chars) succeeds if the current input character is not
     *   in the list of chars. Returns the parsed character.
     */
    fun noneOf(chars: List<Char>): Parser<Char> =
            satisfy{ch: Char -> !chars.contains(ch)}

    fun noneOf(vararg chars: Char): Parser<Char> =
            satisfy{ch: Char -> !chars.contains(ch)}

    /**
     * Skip none or more white space characters.
     */
    val spaces: Parser<Unit>
            get() = skipMany(space).label("white space")

    /**
     * Parse a white space character and return it.
     */
    val space: Parser<Char>
            get() = satisfy{ch: Char -> Character.isWhitespace(ch)}.label("space")

    /**
     * Parse a newline character and return it.
     */
    val newline: Parser<Char>
            get() = character('\n').label("lf newline")

    /**
     * Parse a carriage return character followed by a newline character and
     *   return the newline character.
     */
    val crlf: Parser<Char>
            get() = character('\r').sDF(character('\n')).label("crlf newline")

    /**
     * Parse a newline or carriage return and return it.
     */
    val endOfLine: Parser<Char>
            get() = newline.plus(crlf).label("newline")

    /**
     * Parse a tab character and return it.
     */
    val tab: Parser<Char>
            get() = character('\t').label("tab")

    /**
     * Parse a uppercase character and return it.
     */
    val upper: Parser<Char>
            get() = satisfy{c -> (Character.isUpperCase(c))}.label("uppercase letter")

    /**
     * Parse a lowercase character and return it.
     */
    val lower: Parser<Char>
            get() = satisfy{c -> (Character.isLowerCase(c))}.label("lowercase letter")

    /**
     * Parse an alphabetic or numeric character and return it.
     */
    val alphaNum: Parser<Char>
            get() = satisfy{c -> Character.isLetter(c) || Character.isDigit(c)}.label("letter or digit")

    /**
     * Parse a letter character and return it.
     */
    val letter: Parser<Char>
            get() = satisfy{c -> (Character.isLetter(c))}.label("letter")

    /**
     * Parse a decimal digit character and return it.
     */
    val digit: Parser<Char>
            get() = satisfy{c -> Character.isDigit(c)}.label("digit")

    /**
     * Parse a hexadecimal digit character and return it.
     */
    val hexDigit: Parser<Char>
            get() = satisfy{c -> HEX.contains(c)}.label("hexadecimal digit")

    /**
     * Parse an octal digit character and return it.
     */
    val octDigit: Parser<Char>
            get() = satisfy{c -> OCTAL.contains(c)}.label("octal digit")

    /**
     * The parser string(str) parses a sequence of characters given by str.
     *   Returns the parsed string.
     */
    fun string(str: List<Char>): Parser<List<Char>> =
            tokens(C2(::updateString), str)

    /**
     * The parser string(str) parses a sequence of characters given by str.
     *   Returns the parsed string.
     */
    fun string(str: String): Parser<String> =
            string(ListF.from(str)).fmap{cs: List<Char> -> cs.charsToString()}

    /**
     * Parse an ampersand character and return it.
     */
    val ampersand: Parser<Char>
        get() = satisfy{c -> (c == '&')}

    /**
     * Parse an at character and return it.
     */
    val at: Parser<Char>
        get() = satisfy{c -> (c == '@')}

    /**
     * Parse a backslash character and return it.
     */
    val backslash: Parser<Char>
        get() = satisfy{c -> (c == '\\')}

    /**
     * Parse a backtick character and return it.
     */
    val backtick: Parser<Char>
        get() = satisfy{c -> (c == '`')}

    /**
     * Parse a circumflex character and return it.
     */
    val circumflex: Parser<Char>
        get() = satisfy{c -> (c == '^')}

    /**
     * Parse colon character and return it.
     */
    val colon: Parser<Char>
        get() = satisfy{c -> (c == ':')}

    /**
     * Parse a comma character and return it.
     */
    val comma: Parser<Char>
        get() = satisfy{c -> (c == ',')}

    /**
     * Parse a dollar character and return it.
     */
    val dollar: Parser<Char>
        get() = satisfy{c -> (c == '$')}

    /**
     * Parse  double quote character and return it.
     */
    val doublequote: Parser<Char>
        get() = satisfy{c -> (c == '"')}

    /**
     * Parse an equal character and return it.
     */
    val equal: Parser<Char>
        get() = satisfy{c -> (c == '=')}

    /**
     * Parse an exclamation character and return it.
     */
    val exclamation: Parser<Char>
        get() = satisfy{c -> (c == '!')}

    /**
     * Parse a greater than character and return it.
     */
    val greaterthan: Parser<Char>
        get() = satisfy{c -> (c == '>')}

    /**
     * Parse a hash character and return it.
     */
    val hash: Parser<Char>
        get() = satisfy{c -> (c == '#')}

    /**
     * Parse a left brace character and return it.
     */
    val leftangle: Parser<Char>
        get() = satisfy{c -> (c == '<')}

    /**
     * Parse a left angle character and return it.
     */
    val leftbrace: Parser<Char>
        get() = satisfy{c -> (c == '{')}

    /**
     * Parse left parenthesis character and return it.
     */
    val leftparenthesis: Parser<Char>
        get() = satisfy{c -> (c == '(')}

    /**
     * Parse a left subscript character and return it.
     */
    val leftsubscript: Parser<Char>
        get() = satisfy{c -> (c == '[')}

    /**
     * Parse a less than character and return it.
     */
    val lessthan: Parser<Char>
        get() = satisfy{c -> (c == '<')}

    /**
     * Parse a minus character and return it.
     */
    val minus: Parser<Char>
        get() = satisfy{c -> (c == '-')}

    /**
     * Parse  multiply character and return it.
     */
    val multiply: Parser<Char>
        get() = satisfy{c -> (c == '*')}

    /**
     * Parse a not character and return it.
     */
    val not: Parser<Char>
        get() = satisfy{c -> (c == '!')}

    /**
     * Parse a pipe character and return it.
     */
    val pipe: Parser<Char>
        get() = satisfy{c -> (c == '|')}

    /**
     * Parse a plus character and return it.
     */
    val plus: Parser<Char>
        get() = satisfy{c -> (c == '+')}

    /**
     * Parse a question mark character and return it.
     */
    val question: Parser<Char>
        get() = satisfy{c -> (c == '?')}

    /**
     * Parse a right angle character and return it.
     */
    val rightangle: Parser<Char>
        get() = satisfy{c -> (c == '>')}

    /**
     * Parse  right brace character and return it.
     */
    val rightbrace: Parser<Char>
        get() = satisfy{c -> (c == '}')}

    /**
     * Pars right parenthesis character and return it.
     */
    val rightparenthesis: Parser<Char>
        get() = satisfy{c -> (c == ')')}

    /**
     * Pars right subscript character and return it.
     */
    val rightsubscript: Parser<Char>
        get() = satisfy{c -> (c == ']')}

    /**
     * Parse a percentage character and return it.
     */
    val percentage: Parser<Char>
        get() = satisfy{c -> (c == '%')}

    /**
     * Parse  single quote character and return it.
     */
    val singlequote: Parser<Char>
        get() = satisfy{c -> (c == '\'')}

    /**
     * Parse a semicolon character and return it.
     */
    val semicolon: Parser<Char>
        get() = satisfy{c -> (c == ';')}

    /**
     * Parse a slash character and return it.
     */
    val slash: Parser<Char>
        get() = satisfy{c -> (c == '\\')}

    /**
     * Parse a tilde character and return it.
     */
    val tilde: Parser<Char>
        get() = satisfy{c -> (c == '~')}

    /**
     * Parse an underscore character and return it.
     */
    val underscore: Parser<Char>
        get() = satisfy{c -> (c == '_')}



// ----------implementation -------------------------------

    private val HEX: List<Char> = ListF.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    private val OCTAL: List<Char> = ListF.of('0', '1', '2', '3', '4', '5', '6', '7')

}    // CharacterF
