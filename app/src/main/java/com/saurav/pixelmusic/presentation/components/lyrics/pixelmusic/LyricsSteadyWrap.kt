// LyricsSteadyWrap.kt

package com.saurav.pixelmusic.presentation.components.lyrics.pixelmusic

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.util.concurrent.ConcurrentHashMap

// the sung weight is a little wider than the resting one, so a long resting line breaks where its sung self will and doesn't gain or lose a row when it lights up
@Composable
internal fun rememberSteadyWrap(text: String, boldStyle: TextStyle): SteadyWrap {
    val measurer = rememberTextMeasurer(cacheSize = 1)
    val fonts = LocalFontFamilyResolver.current
    val direction = LocalLayoutDirection.current
    return remember(text, boldStyle, measurer) { SteadyWrap(text, boldStyle, measurer, fonts, direction) }
}

// put the modifier on whatever holds the line at its full width, and show text instead of the raw line
internal class SteadyWrap(
    private val source: String,
    private val boldStyle: TextStyle,
    private val measurer: TextMeasurer,
    private val fonts: FontFamily.Resolver,
    private val direction: LayoutDirection,
) {
    // starts on the wrap already worked out for this line, so it lays out once instead of again on the next frame
    private val shown = mutableStateOf(SteadyWraps.known(source, boldStyle) ?: source)

    val text: String get() = shown.value

    val modifier: Modifier = Modifier.layout { measurable, constraints ->
        if (constraints.hasBoundedWidth && source.isNotEmpty()) {
            val width = constraints.maxWidth
            val steady = SteadyWraps.get(width, this, boldStyle, source) ?: run {
                val bold = measurer.measure(
                    text = source,
                    style = boldStyle,
                    constraints = Constraints(maxWidth = width),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                breakLike(source, bold).also { SteadyWraps.put(width, this, boldStyle, source, it) }
            }
            SteadyWraps.note(width, this, boldStyle, fonts, direction)
            if (steady != shown.value) shown.value = steady
        }
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

// the wraps worked out so far, by the width, density and font they were measured at; songs to come are worked out off the main thread
internal object SteadyWraps {
    // the size and font a kind of line was last laid out at, so the next song's lines can be measured the same way ahead of time
    private class Profile(val width: Int, val density: Density, val style: TextStyle, val fonts: FontFamily.Resolver, val direction: LayoutDirection)

    private val wraps = LruCache<String, String>(1_000)
    private val profiles = ConcurrentHashMap<String, Profile>()

    // only what changes where a line breaks; the alignment and colour don't
    private fun styleKey(style: TextStyle) =
        "${style.fontSize}|${style.letterSpacing}|${style.fontFamily}|${style.fontWeight}|${style.fontFeatureSettings}"

    private fun key(width: Int, density: Density, styleKey: String, text: String) =
        "$width|${density.density}|${density.fontScale}|$styleKey|$text"

    fun get(width: Int, density: Density, style: TextStyle, text: String): String? = wraps.get(key(width, density, styleKey(style), text))

    fun put(width: Int, density: Density, style: TextStyle, text: String, steady: String) {
        wraps.put(key(width, density, styleKey(style), text), steady)
    }

    fun known(text: String, style: TextStyle): String? {
        val sk = styleKey(style)
        val profile = profiles[sk] ?: return null
        return wraps.get(key(profile.width, profile.density, sk, text))
    }

    fun note(width: Int, density: Density, style: TextStyle, fonts: FontFamily.Resolver, direction: LayoutDirection) {
        val sk = styleKey(style)
        val last = profiles[sk]
        if (last != null && last.width == width && last.density.density == density.density && last.density.fontScale == density.fontScale) return
        profiles[sk] = Profile(width, Density(density.density, density.fontScale), style, fonts, direction)
    }

    // called on a background thread with a song's lines; a failure only means those lines work it out themselves later
    @Suppress("unused")
    private fun warm(texts: List<String>) {
        profiles.values.forEach { profile ->
            val sk = styleKey(profile.style)
            val measurer = TextMeasurer(profile.fonts, profile.density, profile.direction, 0)
            texts.forEach { text ->
                val k = key(profile.width, profile.density, sk, text)
                if (text.isEmpty() || wraps.get(k) != null) return@forEach
                runCatching {
                    val bold = measurer.measure(
                        text = text,
                        style = profile.style,
                        constraints = Constraints(maxWidth = profile.width),
                        layoutDirection = profile.direction,
                        density = profile.density,
                    )
                    wraps.put(k, breakLike(text, bold))
                }
            }
        }
    }
}

// the same characters with each space the bold line wraps at turned into a line break, so every offset stays where it was
private fun breakLike(text: String, bold: TextLayoutResult): String {
    if (bold.lineCount <= 1) return text
    var chars: CharArray? = null
    for (line in 0 until bold.lineCount - 1) {
        val at = bold.getLineEnd(line) - 1
        if (at !in text.indices || text[at] != ' ') continue
        val out = chars ?: text.toCharArray().also { chars = it }
        out[at] = '\n'
    }
    return chars?.let { String(it) } ?: text
}
