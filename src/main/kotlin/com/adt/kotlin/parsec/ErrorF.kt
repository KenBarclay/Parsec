package com.adt.kotlin.parsec

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.list.ListF
import com.adt.kotlin.data.immutable.list.append

object ErrorF {

    fun <TOK, U> unknownError(state: State<TOK, U>): ParseError =
        newErrorUnknown(state.position)

    fun errorIsUnknown(parseError: ParseError): Boolean =
        parseError.messages.isEmpty()

    fun newErrorUnknown(sourcePosition: SourcePosition): ParseError =
        ParseError(sourcePosition, ListF.empty())

    fun newErrorMessage(sourcePosition: SourcePosition, msg: Msg): ParseError =
        ParseError(sourcePosition, ListF.singleton(msg))

    fun addErrorMessage(parseError: ParseError, msg: Msg): ParseError =
        ParseError(parseError.sourcePosition, ListF.cons(msg, parseError.messages))

    fun setErrorPosition(parseError: ParseError, sourcePosition: SourcePosition): ParseError =
        ParseError(sourcePosition, parseError.messages)

    fun setErrorMessage(parseError: ParseError, msg: Msg): ParseError =
        ParseError(parseError.sourcePosition, ListF.cons(msg, parseError.messages.filter{m -> (m != msg)}))

    fun mergeError(parseError1: ParseError, parseError2: ParseError): ParseError {
        val sourcePosition1: SourcePosition = parseError1.sourcePosition
        val sourcePosition2: SourcePosition = parseError2.sourcePosition
        val messages1: List<Msg> = parseError1.messages
        val messages2: List<Msg> = parseError2.messages

        return if (messages2.isEmpty() && !messages1.isEmpty())
            parseError1
        else if (messages1.isEmpty() && !messages2.isEmpty())
            parseError2
        else {
            val cmp: Int = sourcePosition1.compareTo(sourcePosition2)
            if (cmp < 0)
                parseError2
            else if (cmp > 0)
                parseError1
            else
                ParseError(sourcePosition1, messages1.append(messages2))
        }
    }   // mergeError

}   // ErrorF
