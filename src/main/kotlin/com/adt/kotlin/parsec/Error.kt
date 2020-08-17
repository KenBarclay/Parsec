package com.adt.kotlin.parsec

import com.adt.kotlin.data.immutable.list.List

sealed class Msg {
    data class SysUnExpect(val message: String) : Msg() {
        override fun toString(): String = "SysUnExpect: $message"
    }
    data class UnExpect(val message: String) : Msg() {
        override fun toString(): String = "UnExpect: $message"
    }
    data class Expect(val message: String) : Msg() {
        override fun toString(): String = "Expect: $message"
    }
    data class Message(val message: String) : Msg() {
        override fun toString(): String = "Message: $message"
    }
}   // Msg

fun message(msg: Msg): String =
    when (msg) {
        is Msg.SysUnExpect -> msg.message
        is Msg.UnExpect -> msg.message
        is Msg.Expect -> msg.message
        is Msg.Message -> msg.message
    }   // message

data class ParseError(val sourcePosition: SourcePosition, val messages: List<Msg>) {

    override fun equals(other: Any?): Boolean {
        return if (this === other)
            true
        else if (other == null || this::class.java != other::class.java)
            false
        else {
            @Suppress("UNCHECKED_CAST") val otherParseError: ParseError = other as ParseError
            (this.sourcePosition == otherParseError.sourcePosition) &&
                    (this.messages.map(::message) == otherParseError.messages.map(::message))
        }

    }   // equals

}   // ParseError

fun <TOK, U, A> ParseError.mergeErrorReply(reply: Reply<TOK, U, A>): Reply<TOK, U, A> =
        when (reply) {
            is Reply.Ok -> Reply.Ok(reply.a, reply.state, ErrorF.mergeError(this, reply.parseError))
            is Reply.Error -> Reply.Error(ErrorF.mergeError(this, reply.parseError))
        }   // mergeErrorReply
