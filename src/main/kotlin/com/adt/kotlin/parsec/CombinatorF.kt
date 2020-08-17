package com.adt.kotlin.parsec

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.list.ListF
import com.adt.kotlin.data.immutable.option.Option
import com.adt.kotlin.data.immutable.option.OptionF.none
import com.adt.kotlin.data.immutable.option.OptionF.some

import com.adt.kotlin.parsec.ParsecTF.inject
import com.adt.kotlin.parsec.ParsecTF.tokenPrim
import com.adt.kotlin.parsec.ParsecTF.unexpected
import com.adt.kotlin.parsec.ParsecTF.zero


/**
 * Commonly used generic combinators.
 */
object CombinatorF {

    /**
     * choice(parsecs) tries to apply the parsers in the list parsecs in order,
     *   until one of them succeeds. Returns the value of the succeeding parser.
     */
    fun <TOK, U, A, Z> choice(parsecs: List<ParsecT<TOK, U, A, Z>>): ParsecT<TOK, U, A, Z> =
            parsecs.foldRight(zero()){acc: ParsecT<TOK, U, A, Z> -> {psec: ParsecT<TOK, U, A, Z> -> acc.choice(psec)}}

    fun <TOK, U, A, Z> choice(vararg parsecs: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, A, Z> =
            parsecs.fold(zero()){acc: ParsecT<TOK, U, A, Z>, psec: ParsecT<TOK, U, A, Z> -> acc.choice(psec)}

    /**
     * option(a, parsec) tries to apply parser parsec. If parsec fails without
     *   consuming input, it returns the value a, otherwise the value returned
     *   by parsec.
     */
    fun <TOK, U, A, Z> option(a: A, parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, A, Z> =
            parsec.choice(inject(a))

    /**
     * optionMaybe(parsec) tries to apply parser parsec. If parsec fails without
     *   consuming input, it returns None, otherwise it returns Some the value
     *   returned by parsec.
     */
    fun <TOK, U, A, Z> optionMaybe(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, Option<A>, Z> =
            option(none(), ParsecTF.liftM<TOK, U, A, Z, Option<A>>{a: A -> some(a)}(parsec))

    /**
     * optional(parsec) tries to apply parser parsec. It will parse parsec or
     *   nothing. It only fails if parsec fails after consuming input. It discards
     *   the result of parsec.
     */
    fun <TOK, U, A, Z> optional(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, Unit, Z> =
            parsec.fmap{_: A -> Unit}.plus(inject(Unit))

    /**
     * between(open, close, parsec) parses open, followed by parsec and close.
     *   Returns the value returned by parsec.
     */
    fun <TOK, U, A, Z, OPEN, CLOSE> between(open: ParsecT<TOK, U, OPEN, Z>, close: ParsecT<TOK, U, CLOSE, Z>, parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, A, Z> =
            open.bind{_: OPEN -> parsec.bind{a: A -> close.bind{_: CLOSE -> inject<TOK, U, A, Z>(a)}}}

    /**
     * skipMany(parsec) applies the parser parsec none or more times, skipping
     *   its result.
     */
    fun <TOK, U, A, Z> skipMany(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, Unit, Z> {
        fun scan(): ParsecT<TOK, U, Unit, Z> =
                parsec.bind{_: A -> scan()}.plus(inject(Unit))
        return scan()
    }   // skipMany

    /**
     * skipMany1(parsec) applies the parser parsec one or more times, skipping
     *   its result.
     */
    fun <TOK, U, A, Z> skipMany1(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, Unit, Z> =
            parsec.bind{_: A -> skipMany(parsec)}

    /**
     * many(parsec) applies the parser parsec none or more times. Returns a
     *   list of the returned values of parsec.
     */
    fun <TOK, U, A, Z> many(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, List<A>, Z> {
        fun scan(f: (List<A>) -> List<A>): ParsecT<TOK, U, List<A>, Z> =
                parsec.bind{a: A -> scan{tail: List<A> -> f(ListF.cons(a, tail))}}.plus(inject(f(ListF.empty())))
        return scan{x -> x}
    }   // many

    /**
     * many1(parsec) applies the parser parsec one or more times. Returns a
     *   list of the returned values of parsec.
     */
    fun <TOK, U, A, Z> many1(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, List<A>, Z> =
            parsec.bind{x: A -> many(parsec).bind{xs: List<A> -> inject<TOK, U, List<A>, Z>(ListF.cons(x, xs))}}

    /**
     * sepBy1(parsec, sep) parses one or more of the occurrences of parsec
     *   separated by sep. Returns a list of values returned by parsec.
     */
    fun <TOK, U, A, Z, SEP> sepBy1(parsec: ParsecT<TOK, U, A, Z>, sep: ParsecT<TOK, U, SEP, Z>): ParsecT<TOK, U, List<A>, Z> =
            parsec.bind{x: A -> many(sep.then(parsec)).bind{xs: List<A> -> inject<TOK, U, List<A>, Z>(ListF.cons(x, xs))}}

    /**
     * sepBy(parsec, sep) parses none or more of the occurrences of parsec
     *   separated by sep. Returns a list of values returned by parsec.
     */
    fun <TOK, U, A, Z, SEP> sepBy(parsec: ParsecT<TOK, U, A, Z>, sep: ParsecT<TOK, U, SEP, Z>): ParsecT<TOK, U, List<A>, Z> =
            sepBy1(parsec, sep).plus(inject(ListF.empty()))

    /**
     * sepEndBy1(parsec, sep) parses one or more occurrences of parsec separated
     *   and optionally ended by sep. Returns the list of values returned by
     *   parsec.
     */
    fun <TOK, U, A, Z, SEP> sepEndBy1(parsec: ParsecT<TOK, U, A, Z>, sep: ParsecT<TOK, U, SEP, Z>): ParsecT<TOK, U, List<A>, Z> =
            parsec.bind{x: A ->
                sep.bind{_: SEP ->
                    sepEndBy(parsec, sep).bind{xs: List<A> ->
                        inject<TOK, U, List<A>, Z>(ListF.cons(x, xs))
                    }
                }.plus(inject(ListF.of(x)))
            }   // sepEndBy1

    /**
     * sepEndBy(parsec, sep) parses none or more occurrences of parsec separated
     *   and optionally ended by sep. Returns the list of values returned by
     *   parsec.
     */
    fun <TOK, U, A, Z, SEP> sepEndBy(parsec: ParsecT<TOK, U, A, Z>, sep: ParsecT<TOK, U, SEP, Z>): ParsecT<TOK, U, List<A>, Z> =
            sepEndBy1(parsec, sep).plus(inject(ListF.empty()))

    /**
     * endBy1(parsec, sep) parses one or more occurrences of parsec separated
     *   and ended by sep. Returns a list of values returned by parsec.
     */
    fun <TOK, U, A, Z, SEP> endBy1(parsec: ParsecT<TOK, U, A, Z>, sep: ParsecT<TOK, U, SEP, Z>): ParsecT<TOK, U, List<A>, Z> =
            many1(parsec.bind{x: A -> sep.bind{_: SEP -> inject<TOK, U, A, Z>(x)}})

    /**
     * endBy(parsec, sep) parses none or more occurrences of parsec separated
     *   and ended by sep. Returns a list of values returned by parsec.
     */
    fun <TOK, U, A, Z, SEP> endBy(parsec: ParsecT<TOK, U, A, Z>, sep: ParsecT<TOK, U, SEP, Z>): ParsecT<TOK, U, List<A>, Z> =
            many(parsec.bind{x: A -> sep.bind{_: SEP -> inject<TOK, U, A, Z>(x)}})

    /**
     * chainRight1(parsec, f) parses none or more occurrences of parsec separated
     *   by f. Returns a value obtained by a right associative application of
     *   all functions returned by f to the values returned by parsec.
     */
    fun <TOK, U, A, Z> chainRight(parsec: ParsecT<TOK, U, A, Z>, f: ParsecT<TOK, U, (A) -> (A) -> A, Z>, a: A): ParsecT<TOK, U, A, Z> =
            chainRight1(parsec, f).plus(inject(a))

    /**
     * chainRight1(parsec, f) parses one or more occurrences of parsec separated
     *   by f. Returns a value obtained by a right associative application of
     *   all functions returned by f to the values returned by parsec.
     */
    fun <TOK, U, A, Z> chainRight1(parsec: ParsecT<TOK, U, A, Z>, f: ParsecT<TOK, U, (A) -> (A) -> A, Z>): ParsecT<TOK, U, A, Z> {
        fun scan(): ParsecT<TOK, U, A, Z> {
            fun rest(x: A): ParsecT<TOK, U, A, Z> =
                    f.bind{op: (A) -> (A) -> A ->
                        scan().bind{y: A ->
                            inject<TOK, U, A, Z>(op(x)(y))
                        }
                    }.plus(inject(x))
            return parsec.bind{x: A -> rest(x)}
        }   // scan

        return scan()
    }   // chainRight1

    /**
     * chainLeft(parsec, f) parses none or more occurrences of parsec separated
     *   by f. Returns a value obtained by a left associative application of
     *   all functions returned by f to the values returned by parsec. If there
     *   are zero occurrences of parsec, the value a is returned.
     */
    fun <TOK, U, A, Z> chainLeft(parsec: ParsecT<TOK, U, A, Z>, f: ParsecT<TOK, U, (A) -> (A) -> A, Z>, a: A): ParsecT<TOK, U, A, Z> =
            chainLeft1(parsec, f).plus(inject(a))

    /**
     * chainLeft1(parsec, f) parses one or more occurrences of parsec separated
     *   by f. Returns a value obtained by a left associative application of
     *   all functions returned by f to the values returned by parsec.
     */
    fun <TOK, U, A, Z> chainLeft1(parsec: ParsecT<TOK, U, A, Z>, f: ParsecT<TOK, U, (A) -> (A) -> A, Z>): ParsecT<TOK, U, A, Z> {
        fun rest(x: A): ParsecT<TOK, U, A, Z> =
                f.bind{op: (A) -> (A) -> A ->
                    parsec.bind{y: A ->
                        rest(op(x)(y))
                    }
                }.plus(inject(x))
        return parsec.bind{x: A -> rest(x)}
    }   // chainLeft1

    /**
     * The parser anyToken accepts any kind of token. It is used, for example,
     *   to implement eof. Returns the accepted token.
     */
    fun <TOK, U, Z> anyToken(): ParsecT<TOK, U, TOK, Z> =
            tokenPrim({pos: SourcePosition -> {tok: TOK -> {str: Stream<TOK> -> pos}}}, {tok: TOK -> some(tok)})

    /**
     * notFollowedBy(parsec) only succeeds when parser parsec fails. This parser
     *   does not consume any input. This parser can be used to implement the
     *   longest match rule. For example when recognising the keyword LET and
     *   the identifier LETTER.
     */
    fun <TOK, U, A, Z> notFollowedBy(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, Unit, Z> =
            parsec.`try`().bind{x: A -> unexpected<TOK, U, Unit, Z>("$x")}.plus(inject(Unit)).`try`()

    /**
     * This parser only succeeds at the end of the input.
     */
    fun <TOK, U, Z> eof(): ParsecT<TOK, U, Unit, Z> =
            notFollowedBy<TOK, U, TOK, Z>(anyToken()).label("end of input")

    /**
     * manyTill(parsec, end) applies parser parsec none or more times until
     *   parser end succeeds. Returns the list of values returned by parsec.
     */
    fun <TOK, U, A, Z, END> manyTill(parsec: ParsecT<TOK, U, A, Z>, end: ParsecT<TOK, U, END, Z>): ParsecT<TOK, U, List<A>, Z> {
        fun scan(): ParsecT<TOK, U, List<A>, Z> =
                end.bind{_: END ->
                    inject<TOK, U, List<A>, Z>(ListF.empty())
                }.plus(parsec.bind{x: A -> scan().bind{xs: List<A> -> inject<TOK, U, List<A>, Z>(ListF.cons(x, xs))}})
        return scan()
    }   // manyTill

}   // CombinatorF
