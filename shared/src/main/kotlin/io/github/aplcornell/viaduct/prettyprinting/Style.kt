package io.github.aplcornell.viaduct.prettyprinting

import org.fusesource.jansi.Ansi

/** One of the 8 basic colors supported by the ANSI standard. */
enum class AnsiBaseColor {
    BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE;

    /** Converts our representation of colors to the one used by the ANSI library. */
    internal fun toAnsiColor(): Ansi.Color =
        ansiColors[this.ordinal]

    companion object {
        /** Caches the call to [values] since it creates a new [Array] each time. */
        private val ansiColors = Ansi.Color.values()
    }
}

/** An [ANSI 4-bit color](https://en.wikipedia.org/wiki/ANSI_escape_code#Colors). */
sealed class AnsiColor

/** The default foreground or background color. */
object DefaultColor : AnsiColor()

/** Normal variant of [baseColor]. */
data class NormalColor(val baseColor: AnsiBaseColor) : AnsiColor()

/** Bright variant of [baseColor]. */
data class BrightColor(val baseColor: AnsiBaseColor) : AnsiColor()

/** A style describing how text should be printed on an ANSI terminal. */
interface Style {
    val foregroundColor: AnsiColor
        get() = DefaultColor

    val backgroundColor: AnsiColor
        get() = DefaultColor

    val italic: Boolean
        get() = false

    val bold: Boolean
        get() = false

    val underline: Boolean
        get() = false
}

/** The default [Style]. */
object DefaultStyle : Style

/** Converts this style description into ANSI escape codes. */
internal fun Style.toAnsi(): Ansi {
    /** Calls [normal] when color is a [NormalColor] and [bright] when it is a [BrightColor]. */
    fun setColor(color: AnsiColor, normal: (Ansi.Color) -> Ansi, bright: (Ansi.Color) -> Ansi) {
        when (color) {
            is DefaultColor ->
                normal(Ansi.Color.DEFAULT)
            is NormalColor ->
                normal(color.baseColor.toAnsiColor())
            is BrightColor ->
                bright(color.baseColor.toAnsiColor())
        }
    }

    val ansi = Ansi()
    setColor(foregroundColor, ansi::fg, ansi::fgBright)
    setColor(backgroundColor, ansi::bg, ansi::bgBright)
    if (italic)
        ansi.a(Ansi.Attribute.ITALIC)
    if (bold)
        ansi.a(Ansi.Attribute.INTENSITY_BOLD)
    if (underline)
        ansi.a(Ansi.Attribute.UNDERLINE)
    return ansi
}
