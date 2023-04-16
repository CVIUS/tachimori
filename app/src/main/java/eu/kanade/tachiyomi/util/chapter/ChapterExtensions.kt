package eu.kanade.tachiyomi.util.chapter

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

val chapterDecimalFormat = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)
