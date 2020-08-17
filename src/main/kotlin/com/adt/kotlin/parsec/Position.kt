package com.adt.kotlin.parsec

typealias SourceName = String
typealias Line = Int
typealias Column = Int

class SourcePosition(val sourceName: SourceName, val line: Line, val column: Column) : Comparable<SourcePosition> {

    override fun toString(): String =
        if (sourceName.isEmpty())
            "($line, $column)"
        else
            "\'$sourceName\'($line, $column)"

    override fun equals(other: Any?): Boolean {
        return if (this === other)
            true
        else if (other == null || this::class.java != other::class.java)
            false
        else {
            @Suppress("UNCHECKED_CAST") val otherSourcePosition: SourcePosition = other as SourcePosition
            if (this.sourceName != otherSourcePosition.sourceName)
                false
            else if (this.line != otherSourcePosition.line)
                false
            else
                (this.column == otherSourcePosition.column)
        }
    }   // equals

    override fun compareTo(other: SourcePosition): Int =
        if (this.line < other.line)
            -1
        else if (this.line > other.line)
            +1
        else if (this.column < other.column)
            -1
        else if (this.column > other.column)
            +1
        else
            0

}   // SourcePosition
