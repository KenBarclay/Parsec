package com.adt.kotlin.parsec

import com.adt.kotlin.data.immutable.list.List

object PositionF {

    fun sourcePosition(sourceName: SourceName, line: Line, column: Column): SourcePosition =
        SourcePosition(sourceName, line, column)

    fun initialSourcePosition(sourceName: SourceName): SourcePosition =
        SourcePosition(sourceName, 1, 1)

    fun advanceLine(sourcePosition: SourcePosition): SourcePosition =
        SourcePosition(sourcePosition.sourceName, sourcePosition.line + 1, sourcePosition.column)

    fun setLine(sourcePosition: SourcePosition, line: Line): SourcePosition =
        SourcePosition(sourcePosition.sourceName, line, sourcePosition.column)

    fun advanceColumnBy(sourcePosition: SourcePosition, column: Column): SourcePosition =
        SourcePosition(sourcePosition.sourceName, sourcePosition.line, sourcePosition.column + column)

    fun setColumn(sourcePosition: SourcePosition, column: Column): SourcePosition =
        SourcePosition(sourcePosition.sourceName, sourcePosition.line, column)

    fun updateString(sourcePosition: SourcePosition, str: String): SourcePosition =
        str.fold(sourcePosition){sPosition, ch -> updateChar(sPosition, ch)}

    fun updateString(sourcePosition: SourcePosition, str: List<Char>): SourcePosition =
            str.foldLeft(sourcePosition){sPosition -> {ch -> updateChar(sPosition, ch)}}

    fun updateChar(sourcePosition: SourcePosition, ch: Char): SourcePosition =
        if (ch == '\n')
            SourcePosition(sourcePosition.sourceName, sourcePosition.line + 1, 1)
        else if (ch == '\t') {
            val col: Column = sourcePosition.column
            SourcePosition(sourcePosition.sourceName, sourcePosition.line, col + 8 - ((col - 1) % 8))
        } else
            SourcePosition(sourcePosition.sourceName, sourcePosition.line, sourcePosition.column + 1)

}   // PositionF
