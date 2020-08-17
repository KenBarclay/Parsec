package com.adt.kotlin.parsec

/**
 * Inputs that can be consumed by the library.
 *
 * @author	                    Ken Barclay
 * @since                       July 2020
 */

import com.adt.kotlin.data.immutable.list.List
import com.adt.kotlin.data.immutable.option.Option
import com.adt.kotlin.data.immutable.option.OptionF.none
import com.adt.kotlin.data.immutable.option.OptionF.some



interface Stream<out TOK> {

    fun uncons(): Option<Pair<TOK, Stream<TOK>>>

}   // Stream



class StringStream(val text: String) : Stream<Char> {

    override fun uncons(): Option<Pair<Char, Stream<Char>>> =
            if (text.isEmpty())
                none()
            else
                some(Pair(text[0], StringStream(text.drop(1))))

}   // StringStream



class ListCharStream(val text: List<Char>) : Stream<Char> {

    override fun uncons(): Option<Pair<Char, Stream<Char>>> =
            if (text.isEmpty())
                none()
            else
                some(Pair(text.head(), ListCharStream(text.tail())))

}   // ListCharStream
