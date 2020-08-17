package com.adt.kotlin.parsec

import com.adt.kotlin.data.immutable.either.Either
import com.adt.kotlin.data.immutable.either.EitherF.left
import com.adt.kotlin.data.immutable.either.EitherF.right

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.list.ListF

import com.adt.kotlin.data.immutable.option.Option
import com.adt.kotlin.data.immutable.option.OptionF.none

import com.adt.kotlin.hkfp.fp.FunctionF.C2
import com.adt.kotlin.hkfp.fp.FunctionF.C3
import com.adt.kotlin.hkfp.fp.FunctionF.flip

import com.adt.kotlin.parsec.ErrorF.newErrorMessage
import com.adt.kotlin.parsec.ErrorF.newErrorUnknown
import com.adt.kotlin.parsec.ErrorF.setErrorMessage
import com.adt.kotlin.parsec.ErrorF.unknownError

import com.adt.kotlin.parsec.Consumption.Consumed
import com.adt.kotlin.parsec.Consumption.Empty
import com.adt.kotlin.parsec.PositionF.initialSourcePosition
import com.adt.kotlin.parsec.Reply.Ok
import com.adt.kotlin.parsec.Reply.Error



object ParsecTF {

    /**
     * The parser unexpected(msg) always fails with an unexpected error msg
     *   without consuming any input.
     */
    fun <TOK, U, A, Z> unexpected(msg: String): ParsecT<TOK, U, A, Z> =
            ParsecT{state: State<TOK, U> ->
                {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {_: (ParseError) -> Identity<Z> ->
                        {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                            {emptyError: (ParseError) -> Identity<Z> ->
                                emptyError(newErrorMessage(state.position, Msg.UnExpect(msg)))
                            }
                        }
                    }
                }
            }   // unexpected

    /**
     * The parser inject(a) succeeds with the value a without consuming any
     *   input.
     */
    fun <TOK, U, A, Z> inject(a: A): ParsecT<TOK, U, A, Z> =
            ParsecT{state: State<TOK, U> ->
                {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {_: (ParseError) -> Identity<Z> ->
                        {emptyOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                            {_: (ParseError) -> Identity<Z> ->
                                emptyOk(a)(state)(unknownError(state))
                            }
                        }
                    }
                }
            }   // inject

    /**
     * The parser parserFail(msg) fails with a message msg without consuming
     *   any input.
     */
    fun <TOK, U, A, Z> parserFail(msg: String): ParsecT<TOK, U, A, Z> =
            ParsecT{state: State<TOK, U> ->
                {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {_: (ParseError) -> Identity<Z> ->
                        {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                            {emptyError: (ParseError) -> Identity<Z> ->
                                emptyError(newErrorMessage(state.position, Msg.Message(msg)))
                            }
                        }
                    }
                }
            }   // parserFail

    /**
     * The parser zero() always fails without consuming any input.
     */
    fun <TOK, U, A, Z> zero(): ParsecT<TOK, U, A, Z> =
            ParsecT{state: State<TOK, U> ->
                {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {_: (ParseError) -> Identity<Z> ->
                        {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                            {emptyError: (ParseError) -> Identity<Z> ->
                                emptyError(unknownError(state))
                            }
                        }
                    }
                }
            }   // zero

    /**
     * Lift a unary function to actions.
     */
    fun <TOK, U, A, Z, B> liftA(f: (A) -> B): (ParsecT<TOK, U, A, Z>) -> ParsecT<TOK, U, B, Z> =
            {parsec: ParsecT<TOK, U, A, Z> ->
                parsec.ap(inject(f))
            }   // liftA

    /**
     * Lift a binary function to actions.
     */
    fun <TOK, U, A, Z, B, C> liftA2(f: (A) -> (B) -> C): (ParsecT<TOK, U, A, Z>) -> (ParsecT<TOK, U, B, Z>) -> ParsecT<TOK, U,C, Z> =
            {parsecA: ParsecT<TOK, U, A, Z> ->
                {parsecB: ParsecT<TOK, U, B, Z> ->
                    parsecB.ap(parsecA.fmap(f))
                }
            }   // liftA2

    /**
     * Map the given function across the two ParsecT.
     */
    fun <TOK, U, A, Z, B, C> fmap2(parsecA: ParsecT<TOK, U, A, Z>, parsecB: ParsecT<TOK, U, B, Z>, f: (A) -> (B) -> C): ParsecT<TOK, U, C, Z> =
            liftA2<TOK, U, A, Z, B, C>(f)(parsecA)(parsecB)

    /**
     * Lift a unary function to a monad.
     */
    fun <TOK, U, A, Z, B> liftM(f: (A) -> B): (ParsecT<TOK, U, A, Z>) -> ParsecT<TOK, U, B, Z> =
            {parsecA: ParsecT<TOK, U, A, Z> ->
                parsecA.bind{a: A -> inject<TOK, U, B, Z>(f(a))}
            }   // liftM

    fun <TOK, U, A, Z, B, C> liftM2(f: (A) -> (B) -> C): (ParsecT<TOK, U, A, Z>) -> (ParsecT<TOK, U, B, Z>) -> ParsecT<TOK, U, C, Z> =
            {parsecA: ParsecT<TOK, U, A, Z> ->
                {parsecB: ParsecT<TOK, U, B, Z> ->
                    parsecA.bind{a: A ->
                        parsecB.bind{b: B ->
                            inject<TOK, U, C, Z>(f(a)(b))
                        }
                    }
                }
            }   // liftM2

    fun <TOK, U, A, Z, B, C, D> liftM3(f: (A) -> (B) -> (C) -> D): (ParsecT<TOK, U, A, Z>) -> (ParsecT<TOK, U, B, Z>) -> (ParsecT<TOK, U, C, Z>) -> ParsecT<TOK, U, D, Z> =
            {parsecA: ParsecT<TOK, U, A, Z> ->
                {parsecB: ParsecT<TOK, U, B, Z> ->
                    {parsecC: ParsecT<TOK, U, C, Z> ->
                        parsecA.bind{a: A ->
                            parsecB.bind{b: B ->
                                parsecC.bind{c: C ->
                                    inject<TOK, U, D, Z>(f(a)(b)(c))
                                }
                            }
                        }
                    }
                }
            }   // liftM3

    fun <TOK, U, A, Z, B, C> mapM2(parsecA: ParsecT<TOK, U, A, Z>, parsecB: ParsecT<TOK, U, B, Z>, f: (A) -> (B) -> C): ParsecT<TOK, U, C, Z> =
            liftM2<TOK, U, A, Z, B, C>(f)(parsecA)(parsecB)

    fun <TOK, U, A, Z, B, C, D> mapM3(parsecA: ParsecT<TOK, U, A, Z>, parsecB: ParsecT<TOK, U, B, Z>, parsecC: ParsecT<TOK, U, C, Z>, f: (A) -> (B) -> (C) -> D): ParsecT<TOK, U, D, Z> =
            liftM3<TOK, U, A, Z, B, C, D>(f)(parsecA)(parsecB)(parsecC)

    /**
     * The parser tokenPrimEx(nextPos, op, test) accepts a token t with result x
     *   when the function test(t) returns Some(x). The position of the next token
     *   should be returned when nextPos is called with current source position
     *   pos, the current token t and the remainder of the tokens toks. The op
     *   is used to produce the new user value.
     */
    fun <TOK, U, A, Z> tokenPrimEx(
            nextPos: (SourcePosition) -> (TOK) -> (Stream<TOK>) -> SourcePosition,
            op: Option<(SourcePosition) -> (TOK) -> (Stream<TOK>) -> (U) -> U>,
            test: (TOK) -> Option<A>
    ): ParsecT<TOK, U, A, Z> {
        return when (op) {
            is Option.None -> {
                ParsecT{state: State<TOK, U> ->
                    {consumedOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                        {_: (ParseError) -> Identity<Z> ->
                            {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                                {emptyError: (ParseError) -> Identity<Z> ->
                                    val r: Option<Pair<TOK, Stream<TOK>>> = state.input.uncons()
                                    when (r) {
                                        is Option.None -> emptyError(unexpectError(state.position, ""))
                                        is Option.Some -> {
                                            val (hd: TOK, tl: Stream<TOK>) = r.value
                                            when (val testhd = test(hd)) {
                                                is Option.None -> emptyError(unexpectError(state.position, "$hd"))
                                                is Option.Some -> {
                                                    val newPos: SourcePosition = nextPos(state.position)(hd)(tl)
                                                    val newState = State(tl, newPos, state.user)
                                                    consumedOk(testhd.value)(newState)(newErrorUnknown(newPos))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is Option.Some -> {
                ParsecT{state: State<TOK, U> ->
                    {consumedOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                        {_: (ParseError) -> Identity<Z> ->
                            {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                                {emptyError: (ParseError) -> Identity<Z> ->
                                    val r: Option<Pair<TOK, Stream<TOK>>> = state.input.uncons()
                                    when (r) {
                                        is Option.None -> emptyError(unexpectError(state.position, ""))
                                        is Option.Some -> {
                                            val (hd: TOK, tl: Stream<TOK>) = r.value
                                            when (val testhd = test(hd)) {
                                                is Option.None -> emptyError(unexpectError(state.position, "$hd"))
                                                is Option.Some -> {
                                                    val newPos = nextPos(state.position)(hd)(tl)
                                                    val newUser = op.value(state.position)(hd)(tl)(state.user)
                                                    val newState = State(tl, newPos, newUser)
                                                    consumedOk(testhd.value)(newState)(newErrorUnknown(newPos))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }   // tokenPrimEx

    /**
     * The parser tokenPrim(nextPos, test) accepts a token t with result x when
     *   the function test(t) returns Some(x). The position of the next token
     *   should be returned when nextPos is called with current source position
     *   pos, the current token t and the remainder of the tokens toks.
     */
    fun <TOK, U, A, Z> tokenPrim(nextPos: (SourcePosition) -> (TOK) -> (Stream<TOK>) -> SourcePosition, test: (TOK) -> Option<A>): ParsecT<TOK, U, A, Z> =
            tokenPrimEx(nextPos, none(), test)

    /**
     * The parser token(tokpos, test) accepts a token t with result x when the
     *   function test(t) returns Some(x). The source position of the t should
     *   be returned by tokpos(t).
     */
    fun <TOK, U, A, Z> token(tokpos: (TOK) -> SourcePosition, test: (TOK) -> Option<A>): ParsecT<TOK, U, A, Z> {
        fun nextPos(sourcePosition: SourcePosition, tok: TOK, stream: Stream<TOK>): SourcePosition =
                when (val un = stream.uncons()) {
                    is Option.None -> tokpos(tok)
                    is Option.Some -> {
                        val r: Pair<TOK, Stream<TOK>> = un.value
                        tokpos(r.first)
                    }
                }

        return tokenPrim(C3(::nextPos), test)
    }   // token

    /**
     * The parser tokens(nextPos, toks) delivers a parser to recognise the list of toks.
     */
    fun <TOK, U, A, Z> tokens(nextPos: (SourcePosition) -> (List<A>) -> SourcePosition, toks: List<A>): ParsecT<TOK, U, List<A>, Z> =
        if (toks.isEmpty())
            ParsecT{state: State<TOK, U> ->
                {_: (List<A>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {_: (ParseError) -> Identity<Z> ->
                        {emptyOk: (List<A>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                            {_: (ParseError) -> Identity<Z> ->
                                emptyOk(ListF.empty())(state)(unknownError(state))
                            }
                        }
                    }
                }
            }
        else
            ParsecT{state: State<TOK, U> ->
                {consumedOk: (List<A>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {consumedError: (ParseError) -> Identity<Z> ->
                        {_: (List<A>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                            {emptyError: (ParseError) -> Identity<Z> ->
                                val setErrorM: (Msg) -> (ParseError) -> ParseError = flip(C2(::setErrorMessage))
                                val errEof = setErrorM(Msg.Expect("$toks"))(newErrorMessage(state.position, Msg.SysUnExpect("")))
                                fun errExpect(tok: TOK) = setErrorM(Msg.Expect("$toks"))(newErrorMessage(state.position, Msg.SysUnExpect("$tok")))
                                fun ok(st: Stream<TOK>): Identity<Z> {
                                    val newPos: SourcePosition = nextPos(state.position)(toks)
                                    val newState: State<TOK, U> = State(st, newPos, state.user)
                                    return consumedOk(toks)(newState)(newErrorUnknown(newPos))
                                }   // ok
                                fun walk(ts: List<A>, st: Stream<TOK>): Identity<Z> =
                                        if (ts.isEmpty())
                                            ok(st)
                                        else {
                                            val sr: Option<Pair<TOK, Stream<TOK>>> = st.uncons()
                                            when (sr) {
                                                is Option.None -> consumedError(errEof)
                                                is Option.Some -> {
                                                    val (x: TOK, xs: Stream<TOK>) = sr.value
                                                    if (ts.head() == x)
                                                        walk(ts.tail(), xs)
                                                    else
                                                        consumedError(errExpect(x))
                                                }
                                            }
                                        }
                                val sr: Option<Pair<TOK, Stream<TOK>>> = state.input.uncons()
                                when (sr) {
                                    is Option.None -> emptyError(errEof)
                                    is Option.Some -> {
                                        val (x: TOK, xs: Stream<TOK>) = sr.value
                                        if (toks.head() == x)
                                            walk(toks.tail(), xs)
                                        else
                                            emptyError(errExpect(x))
                                    }
                                }
                            }
                        }
                    }
                }
            }   // tokens

    /**
     * Run a parser (monadic).
     */
    fun <TOK, U, A, Z> runPT(parsec: ParsecT<TOK, U, A, Z>, u: U, sourceName: SourceName, stream: Stream<TOK>): Either<ParseError, A> {
        fun <R> parserReply(consumption: Consumption<R>): R =
                when (consumption) {
                    is Consumed -> consumption.a
                    is Empty -> consumption.a
                }   // parserReply

        val identity: Identity<Z> = runParsecT(parsec, State(stream, initialSourcePosition(sourceName), u))
        @Suppress("UNCHECKED_CAST") val consumption: Consumption<Identity<Reply<TOK, U, A>>> = identity.value as Consumption<Identity<Reply<TOK, U, A>>>
        val iden: Identity<Reply<TOK, U, A>> = parserReply(consumption)
        val reply: Reply<TOK, U, A> = iden.value

        return when(reply) {
            is Ok -> right(reply.a)
            is Error -> left(reply.parseError)
        }
    }   // runPT

    /**
     * Run a parser (pure).
     */
    fun <TOK, U, A, Z> runP(parsec: ParsecT<TOK, U, A, Z>, u: U, sourceName: SourceName, stream: Stream<TOK>): Either<ParseError, A> =
            runPT(parsec, u, sourceName, stream)



// ---------- implementation ------------------------------

    private fun unexpectError(sourcePosition: SourcePosition, msg: String): ParseError =
            newErrorMessage(sourcePosition, Msg.SysUnExpect(msg))

    private fun <TOK, U, A, Z> runParsecT(parsec: ParsecT<TOK, U, A, Z>, state: State<TOK, U>): Identity<Z> {
        fun cok(a: A, s: State<TOK, U>, err: ParseError): Identity<Z> {
            @Suppress("UNCHECKED_CAST") val z: Z = Consumed(Identity(Ok(a, s, err))) as Z
            return Identity(z)
        }
        fun cerr(err: ParseError): Identity<Z> {
            @Suppress("UNCHECKED_CAST") val z: Z = Consumed(Identity(Error<TOK, U, A>(err))) as Z
            return Identity(z)
        }
        fun eok(a: A, s: State<TOK, U>, err: ParseError): Identity<Z> {
            @Suppress("UNCHECKED_CAST") val z: Z = Empty(Identity(Ok(a, s, err))) as Z
            return Identity(z)
        }
        fun eerr(err: ParseError): Identity<Z> {
            @Suppress("UNCHECKED_CAST") val z: Z = Empty(Identity(Error<TOK, U, A>(err))) as Z
            return Identity(z)
        }

        return parsec.run(state)(C3(::cok))(::cerr)(C3(::eok))(::eerr)
    }   // runParsecT

}   // ParsecTF
