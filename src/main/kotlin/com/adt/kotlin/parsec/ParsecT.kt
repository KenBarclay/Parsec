package com.adt.kotlin.parsec

/**
 * Inputs that can be consumed by the library.
 *
 * @author	                    Ken Barclay
 * @since                       July 2020
 */

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.list.ListF

import com.adt.kotlin.hkfp.fp.FunctionF.C3
import com.adt.kotlin.hkfp.fp.FunctionF.C4
import com.adt.kotlin.hkfp.fp.FunctionF.compose

import com.adt.kotlin.parsec.ErrorF.addErrorMessage
import com.adt.kotlin.parsec.ErrorF.errorIsUnknown
import com.adt.kotlin.parsec.ErrorF.mergeError
import com.adt.kotlin.parsec.ErrorF.newErrorUnknown
import com.adt.kotlin.parsec.ErrorF.setErrorMessage
import com.adt.kotlin.parsec.ErrorF.unknownError
import com.adt.kotlin.parsec.ParsecTF.inject

import java.lang.Exception



class Identity<out A>(val value: A)


/**
 * The parser state parameterised over the input stream token type and the
 *   user type.
 */
class State<out TOK, out U>(val input: Stream<TOK>, val position: SourcePosition, val user: U)


/**
 * A parse can be successful and is an instance of Ok. A failed parse is an
 *   instance of Error.
 */
sealed class Reply<out TOK, out U, out A> {
    data class Ok<out TOK, out U, out A>(val a: A, val state: State<TOK, U>, val parseError: ParseError) : Reply<TOK, U, A>()
    data class Error<out TOK, out U, out A>(val parseError: ParseError) : Reply<TOK, U, A>()
}   // Reply

fun <TOK, U, A, B> Reply<TOK, U, A>.fmap(f: (A) -> B): Reply<TOK, U, B> =
        when (this) {
            is Reply.Ok -> Reply.Ok(f(this.a), this.state, this.parseError)
            is Reply.Error -> Reply.Error(this.parseError)
        }   // fmap


/**
 * A parse can consume some input as is an instance of Consumed. A parser
 *   that consumes no input is an instance of Empty. The generic type A is
 *   an instance of Reply. This produces the four combinations: consumed ok,
 *   consumed error, empty ok and empty error.
 */
sealed class Consumption<out A> {
    data class Consumed<out A>(val a: A) : Consumption<A>()
    data class Empty<out A>(val a: A) : Consumption<A>()
}   // Consumption

fun <A, B> Consumption<A>.fmap(f: (A) -> B): Consumption<B> =
    when (this) {
        is Consumption.Consumed -> Consumption.Consumed(f(this.a))
        is Consumption.Empty -> Consumption.Empty(f(this.a))
    }   // fmap


/**
 * ParsecT TOK, U, A, Z is a parser with stream type TOK, user state type U,
 *   underlying Identity monad of type Z and return type A. ParsecT is lazy
 *   and is represented by run function. Operations on a ParsecT object create
 *   a new ParsecT instance with transformations applied to the underlying
 *   run function.
 *
 * The consumed ok sub-function represents a successful parse with some input
 *   consumed. The consumed error sub-function represents a failed parse with
 *   some input consumed. The empty ok sub-function represents a successful
 *   parse without consuming any input. The empty error sub-function represents
 *   a failed parse without consuming any input.
 */
class ParsecT<TOK, U, A, Z>(
        val run: (State<TOK, U>) ->
        ((A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z>) ->      // consumed ok
        ((ParseError) -> Identity<Z>) ->                                // consumed error
        ((A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z>) ->      // empty ok
        ((ParseError) -> Identity<Z>) ->                                // empty error
        Identity<Z>
) {

    operator fun invoke(state: State<TOK, U>):
            ((A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z>) ->      // consumed ok
            ((ParseError) -> Identity<Z>) ->                                // consumed error
            ((A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z>) ->      // empty ok
            ((ParseError) -> Identity<Z>) ->                                // empty error
            Identity<Z> =
            run(state)

}   // ParsecT



// ---------- Functor -------------------------------------

/**
 * Apply the function to the content(s) of the ParsecT context.
 *   Function fmap applies the function parameter to each item in this list, delivering
 *   a new list. The result list has the same size as this list.
 */
fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.fmap(f: (A) -> B): ParsecT<TOK, U, B, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {consumedOk: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {consumedError: (ParseError) -> Identity<Z> ->
                {emptyOk: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {emptyError: (ParseError) -> Identity<Z> ->
                        self.run(state)(compose(consumedOk, f))(consumedError)(compose(emptyOk, f))(emptyError)
                    }
                }
            }
        }
    }
}   // fmap

/**
 * An infix symbol for fmap.
 */
infix fun <TOK, U, A, Z, B> ((A) -> B).dollar(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, B, Z> =
        parsec.fmap(this)

/**
 * Replace all locations in the input with the given value.
 */
fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.replaceAll(b: B): ParsecT<TOK, U, B, Z> =
        this.fmap{_: A -> b}



// ---------- Applicative functor -------------------------

/**
 * Apply the function wrapped in a ParsecT context to the content of the
 *   receiving value also wrapped in a ParsecT context.
 */
fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.ap(f: ParsecT<TOK, U, (A) -> B, Z>): ParsecT<TOK, U, B, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {consumedOk: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {consumedError: (ParseError) -> Identity<Z> ->
                {emptyOk: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {emptyError: (ParseError) -> Identity<Z> ->
                        fun mcok(a: A, s: State<TOK, U>, err: ParseError): Identity<Z> {
                            val pcok: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = consumedOk
                            val pcokN: ((A) -> B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = {g -> {s -> {er -> pcok(g(a))(s)(er)}}}
                            val pcerr: (ParseError) -> Identity<Z> = consumedError
                            fun peok(b: B, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                    consumedOk(b)(s)(mergeError(err, er))
                            val peokN: ((A) -> B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = {g -> {s -> {er -> peok(g(a), s, er)}}}
                            fun peerr(er: ParseError): Identity<Z> =
                                    consumedError(mergeError(err, er))
                            return f(s)(pcokN)(pcerr)(peokN)(::peerr)
                        }   // mcok
                        fun meok(a: A, s: State<TOK, U>, err: ParseError): Identity<Z> {
                            val pcok: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = consumedOk
                            val pcokN: ((A) -> B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = {g -> {s -> {er -> pcok(g(a))(s)(er)}}}
                            val pcerr: (ParseError) -> Identity<Z> = consumedError
                            fun peok(b: B, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                    emptyOk(b)(s)(mergeError(err, er))
                            val peokN: ((A) -> B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = {g -> {s -> {er -> peok(g(a), s, er)}}}
                            fun peerr(er: ParseError): Identity<Z> =
                                    emptyError(mergeError(err, er))
                            return f(s)(pcokN)(pcerr)(peokN)(::peerr)
                        }   // meok
                        val mcerr: (ParseError) -> Identity<Z> = consumedError
                        val meerr: (ParseError) -> Identity<Z> = emptyError
                        self(state)(C3(::mcok))(mcerr)(C3(::meok))(meerr)
                    }
                }
            }
        }
    }
}   // ap

/**
 * Applicative style for ap.
 */
infix fun <TOK, U, A, Z, B> ParsecT<TOK, U, (A) -> B, Z>.appliedOver(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, B, Z> =
        parsec.ap(this)

/**
 * Sequence actions discarding the result from the recipient.
 */
fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.sDF(parsecB: ParsecT<TOK, U, B, Z>): ParsecT<TOK, U, B, Z> {
    return this.bind{_: A -> parsecB}
}   // sDF

/**
 * Sequence actions discarding the result from the parameter.
 */
fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.sDS(parsecB: ParsecT<TOK, U, B, Z>): ParsecT<TOK, U, A, Z> =
        this.bind{a: A -> parsecB.bind{_: B -> inject<TOK, U, A, Z>(a)}}



// ---------- Monad ---------------------------------------

/**
 * Sequentially compose two actions, passing any value produced by the recipient
 *   as an argument to the second (parameter).
 */
fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.bind(f: (A) -> ParsecT<TOK, U, B, Z>): ParsecT<TOK, U, B, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {consumedOk: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {consumedError: (ParseError) -> Identity<Z> ->
                {emptyOk: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {emptyError: (ParseError) -> Identity<Z> ->
                        fun mcok(a: A, s: State<TOK, U>, err: ParseError): Identity<Z> {
                            val pcok: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = consumedOk
                            val pcerr: (ParseError) -> Identity<Z> = consumedError
                            fun peok(b: B, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                    consumedOk(b)(s)(mergeError(err, er))
                            fun peerr(er: ParseError): Identity<Z> =
                                    consumedError(mergeError(err, er))
                            return f(a)(s)(pcok)(pcerr)(C3(::peok))(::peerr)
                        }   // mcok
                        fun meok(a: A, s: State<TOK, U>, err: ParseError): Identity<Z> {
                            val pcok: (B) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> = consumedOk
                            val pcerr: (ParseError) -> Identity<Z> = consumedError
                            fun peok(b: B, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                    emptyOk(b)(s)(mergeError(err, er))
                            fun peerr(er: ParseError): Identity<Z> =
                                    emptyError(mergeError(err, er))
                            return f(a)(s)(pcok)(pcerr)(C3(::peok))(::peerr)
                        }   // meok
                        val mcerr: (ParseError) -> Identity<Z> = consumedError
                        val meerr: (ParseError) -> Identity<Z> = emptyError
                        self(state)(C3(::mcok))(mcerr)(C3(::meok))(meerr)
                    }
                }
            }
        }
    }
}   // bind

fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.flatMap(f: (A) -> ParsecT<TOK, U, B, Z>): ParsecT<TOK, U, B, Z> =
        this.bind(f)

fun <TOK, U, A, Z, B> ParsecT<TOK, U, A, Z>.then(parsec: ParsecT<TOK, U, B, Z>): ParsecT<TOK, U, B, Z> =
        this.bind{_: A -> parsec}



// ---------- MonadPlus -----------------------------------

/**
 *
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.plus(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, A, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {consumedOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {consumedError: (ParseError) -> Identity<Z> ->
                {emptyOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {emptyError: (ParseError) -> Identity<Z> ->
                        fun meerr(err: ParseError): Identity<Z> {
                            fun neok(a: A, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                    emptyOk(a)(s)(mergeError(err, er))
                            fun neerr(er: ParseError): Identity<Z> =
                                    emptyError(mergeError(err, er))
                            return parsec(state)(consumedOk)(consumedError)(C3(::neok))(::neerr)
                        }   // meerr

                        self(state)(consumedOk)(consumedError)(emptyOk)(::meerr)
                    }
                }
            }
        }
    }
}   // plus

/**
 * This combinator implements choice. The parser p.choice(q) first applies p.
 *   If it succeeds, the value of p is returned. If p fails without consuming
 *   any input then parser q is tried.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.choice(parsec: ParsecT<TOK, U, A, Z>): ParsecT<TOK, U, A, Z> =
        this.plus(parsec)


/**
 *
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.labels(msgs: List<String>): ParsecT<TOK, U, A, Z> {
    fun setExpectErrors(err: ParseError, msgs: List<String>): ParseError =
            if (msgs.isEmpty())
                setErrorMessage(err, Msg.Expect(""))
            else if (msgs.size() == 1)
                setErrorMessage(err, Msg.Expect(msgs[0]))
            else {
                val hd: String = msgs.head()
                val tl: List<String> = msgs.tail()
                tl.foldRight(setErrorMessage(err, Msg.Expect(hd))){msg: String ->
                    {er: ParseError ->
                        addErrorMessage(er, Msg.Expect(msg))
                    }
                }
            }   // setExpectErrors

    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {consumedOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {consumedError: (ParseError) -> Identity<Z> ->
                {emptyOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {emptyError: (ParseError) -> Identity<Z> ->
                        fun meok(a: A, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                emptyOk(a)(s)(if (errorIsUnknown(er)) er else setExpectErrors(er, msgs))
                        fun meerr(er: ParseError): Identity<Z> = emptyError(setExpectErrors(er, msgs))
                        self(state)(consumedOk)(consumedError)(C3(::meok))(::meerr)
                    }
                }
            }
        }
    }
}   // labels

/**
 *
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.label(msg: String): ParsecT<TOK, U, A, Z> =
        this.labels(ListF.singleton(msg))


/**
 * The parser p.try() behaves like parser p except that it pretends that it
 *   has not consumed any input when an error occurs.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.`try`(): ParsecT<TOK, U, A, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {consumedOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {_: (ParseError) -> Identity<Z> ->
                {emptyOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {emptyError: (ParseError) -> Identity<Z> ->
                        self(state)(consumedOk)(emptyError)(emptyOk)(emptyError)
                    }
                }
            }
        }
    }
}   // try


/**
 * The parser p.lookAhead() parses p without consuming any input. If p fails
 *   and consumes some input then so does lookAhead. Combine with type if this
 *   is undesirable.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.lookAhead(): ParsecT<TOK, U, A, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {_: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {consumedError: (ParseError) -> Identity<Z> ->
                {emptyOk: (A) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {emptyError: (ParseError) -> Identity<Z> ->
                        fun meok(a: A, s: State<TOK, U>, err: ParseError): Identity<Z> =
                                emptyOk(a)(s)(newErrorUnknown(s.position))
                        self(state)(C3(::meok))(consumedError)(C3(::meok))(emptyError)
                    }
                }
            }
        }
    }
}   // lookAhead



/**
 * The parser p.manyAccum(...) runs the parser p repeatedly accumulating the
 *   values produced by each run of p.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.manyAccum(acc: (A) -> (List<A>) -> List<A>): ParsecT<TOK, U, List<A>, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {consumedOk: (List<A>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {consumedError: (ParseError) -> Identity<Z> ->
                {emptyOk: (List<A>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {_: (ParseError) -> Identity<Z> ->
                        fun manyErrOk(a: A, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                throw Exception("combinator 'many' is applied to a parser that accepts an empty string")
                        fun walk(xs: List<A>, x: A, s: State<TOK, U>, er: ParseError): Identity<Z> =
                                self(s)(C4(::walk)(acc(x)(xs)))(consumedError)(C3(::manyErrOk))({e: ParseError -> consumedOk(acc(x)(xs))(s)(e)})

                        self(state)(C4(::walk)(ListF.empty()))(consumedError)(C3(::manyErrOk))({er: ParseError -> emptyOk(ListF.empty())(state)(er)})
                    }
                }
            }
        }
    }
}   // manyAccum

/**
 * The parser p.many() runs the parser p none or more times accumulating the
 *   values produced by each run of p.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.many(): ParsecT<TOK, U, List<A>, Z> =
        this.manyAccum{x -> {xs -> ListF.cons(x, xs)}}.fmap{xs -> xs.reverse()}

/**
 * The parser p.many() runs the parser p none or more times skipping the
 *   values produced by each run of p.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.skipMany(): ParsecT<TOK, U, Unit, Z> =
        this.manyAccum{_: A -> {_: List<A> -> ListF.empty()}}.fmap{_: List<A> -> Unit}


/**
 * Apply the given function to the current parser state.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.updateParserState(f: (State<TOK, U>) -> State<TOK, U>): ParsecT<TOK, U, State<TOK, U>, Z> {
    val self: ParsecT<TOK, U, A, Z> = this
    return ParsecT{state: State<TOK, U> ->
        {_: (State<TOK, U>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
            {_: (ParseError) -> Identity<Z> ->
                {emptyOk: (State<TOK, U>) -> (State<TOK, U>) -> (ParseError) -> Identity<Z> ->
                    {_: (ParseError) -> Identity<Z> ->
                        val newState: State<TOK, U> = f(state)
                        emptyOk(newState)(newState)(unknownError(newState))
                    }
                }
            }
        }
    }
}   // updateParserState

/**
 * Return the full parser state.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.getParserState(): ParsecT<TOK, U, State<TOK, U>, Z> =
        this.updateParserState{st: State<TOK, U> -> st}

/**
 * Set the full parser state to that given.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.setParserState(state: State<TOK, U>): ParsecT<TOK, U, State<TOK, U>, Z> =
        this.updateParserState{_: State<TOK, U> -> state}

/**
 * Return the current source position.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.getPosition(): ParsecT<TOK, U, SourcePosition, Z> =
        this.getParserState().bind{state: State<TOK, U> ->
            inject<TOK, U, SourcePosition, Z>(state.position)
        }   // getPosition

/**
 * Set the current source position to the given value.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.setPosition(position: SourcePosition): ParsecT<TOK, U, Unit, Z> =
        this.updateParserState{state: State<TOK, U> -> State(state.input, position, state.user)}.bind{
            inject<TOK, U, Unit, Z>(Unit)
        }   // setPosition

/**
 * Return the current input.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.getInput(): ParsecT<TOK, U, Stream<TOK>, Z> =
        this.getParserState().bind{state: State<TOK, U> ->
            inject<TOK, U, Stream<TOK>, Z>(state.input)
        }   // getInput

/**
 * Set the current input to that given.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.setInput(input: Stream<TOK>): ParsecT<TOK, U, Unit, Z> =
        this.updateParserState{state: State<TOK, U> -> State(input, state.position, state.user)}.bind{
            inject<TOK, U, Unit, Z>(Unit)
        }   // setInput

/**
 * Return the current user state.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.getState(): ParsecT<TOK, U, U, Z> =
        this.getParserState().bind{state: State<TOK, U> ->
            inject<TOK, U, U, Z>(state.user)
        }   // getState

/**
 * Set the user state to the given value.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.putState(user: U): ParsecT<TOK, U, Unit, Z> =
        this.updateParserState{state: State<TOK, U> -> State(state.input, state.position, user)}.bind{
            inject<TOK, U, Unit, Z>(Unit)
        }   // putState

/**
 * Apply the given function to the current user state.
 */
fun <TOK, U, A, Z> ParsecT<TOK, U, A, Z>.modifyState(f: (U) -> U): ParsecT<TOK, U, Unit, Z> =
        this.updateParserState{state: State<TOK, U> -> State(state.input, state.position, f(state.user))}.bind{
            inject<TOK, U, Unit, Z>(Unit)
        }   // modifyState
