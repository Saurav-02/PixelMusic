package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

// ---------------------------------------------------------------------------------------------
// Hero shapes
// ---------------------------------------------------------------------------------------------

private const val HERO_BLOB_COUNT = 4
private val PI_F = PI.toFloat()
private val TWO_PI_F = (2.0 * PI).toFloat()
private val DEG_TO_RAD = (PI / 180.0).toFloat()

// Spring constants (stiffness = w^2, damping = 2 * zeta * w)
private const val ENERGY_STIFFNESS = 30f
private const val ENERGY_DAMPING = 8.8f
private const val KICK_STIFFNESS = 225f
private const val KICK_DAMPING = 9.6f
private const val MORPH_STIFFNESS = 90f
private const val MORPH_DAMPING = 8f

private val HERO_IDLE_SHAPES = listOf(
    AiExpressiveShapeType.PILL,
    AiExpressiveShapeType.COOKIE,
    AiExpressiveShapeType.ROUNDED_TRIANGLE,
    AiExpressiveShapeType.ROUNDED_SQUARE
)

private val HERO_CYCLE_SHAPES = listOf(
    AiExpressiveShapeType.COOKIE,
    AiExpressiveShapeType.SUNNY_COOKIE,
    AiExpressiveShapeType.FIVE_COOKIE,
    AiExpressiveShapeType.ROUNDED_TRIANGLE,
    AiExpressiveShapeType.SIX_COOKIE,
    AiExpressiveShapeType.CIRCLE,
    AiExpressiveShapeType.FOUR_HEAD_COOKIE,
    AiExpressiveShapeType.OVAL,
    AiExpressiveShapeType.ROUNDED_SQUARE,
    AiExpressiveShapeType.PILL
)

private class HeroBlob(val index: Int) {
    val from: FloatArray = HERO_IDLE_SHAPES[index].radii.copyOf()
    val to: FloatArray = HERO_IDLE_SHAPES[index].radii.copyOf()

    /** Morph progress from [from] to [to]; springs past 1 for a little overshoot. */
    var t = 1f
    var v = 0f
    var rotation = 0f
    var cursor = index * 3
    var atIdle = true
    var switchAt = 0f

    // Resolved each frame by the draw pass
    var px = 0f
    var py = 0f
    var pr = 0f
    var prot = 0f

    /** Bakes the current silhouette into [from] so retargeting never pops. */
    fun retarget(next: FloatArray, impulse: Float) {
        for (k in from.indices) from[k] += (to[k] - from[k]) * t
        next.copyInto(to)
        t = 0f
        v = impulse
    }
}

/**
 * Small spring simulation driving the hero shapes. It is stepped once per frame and only read from
 * the draw phase, so the animation never triggers recomposition.
 */
private class HeroSim {
    val tick = mutableLongStateOf(0L)
    val blobs = Array(HERO_BLOB_COUNT) { HeroBlob(it) }

    var time = 0f
    var energy = 0f
    var kick = 0f
    var celebration = 0f
    var celebrating = false

    private var energyVel = 0f
    private var kickVel = 0f
    private var wasActive = false

    fun nudge(impulse: Float) {
        kickVel += impulse
    }

    fun step(dt: Float, generating: Boolean, succeeded: Boolean) {
        val active = generating && !succeeded
        time += dt * (1f + 1.3f * energy)

        val energyTarget = if (active) 1f else 0f
        energyVel += (-ENERGY_STIFFNESS * (energy - energyTarget) - ENERGY_DAMPING * energyVel) * dt
        energy = (energy + energyVel * dt).coerceIn(0f, 1.12f)

        kickVel += (-KICK_STIFFNESS * kick - KICK_DAMPING * kickVel) * dt
        kick += kickVel * dt

        if (active != wasActive) {
            wasActive = active
            for (b in blobs) b.switchAt = time + b.index * 0.13f
        }

        for (b in blobs) {
            if (active) {
                if (time >= b.switchAt) {
                    b.retarget(HERO_CYCLE_SHAPES[b.cursor % HERO_CYCLE_SHAPES.size].radii, 1.4f)
                    b.cursor++
                    b.atIdle = false
                    b.switchAt = time + 1.3f + b.index * 0.21f
                }
            } else if (!b.atIdle && time >= b.switchAt) {
                b.retarget(HERO_IDLE_SHAPES[b.index].radii, 1f)
                b.atIdle = true
            }

            b.v += (-MORPH_STIFFNESS * (b.t - 1f) - MORPH_DAMPING * b.v) * dt
            b.t += b.v * dt

            val spin = (46f + 18f * b.index) * (if (b.index % 2 == 0) 1f else -1f)
            b.rotation += dt * spin * energy
            if (!active) {
                val goal = round(b.rotation / 360f) * 360f
                b.rotation += (goal - b.rotation) * (1f - exp(-dt * 3.2f))
            }
        }

        if (succeeded) {
            celebrating = true
            celebration = min(1f, celebration + dt / 1.15f)
        } else {
            celebrating = false
            celebration = 0f
        }

        tick.longValue += 1L
    }
}

private class HeroPalette(
    val base: List<Color>,
    val highlight: List<Color>,
    val deep: List<Color>,
    val ambient: Color
)

private fun buildHeroPalette(
    primary: Color,
    tertiary: Color,
    secondary: Color,
    primaryContainer: Color,
    isDark: Boolean
): HeroPalette {
    val fourth = lerp(primaryContainer, primary, if (isDark) 0.35f else 0.50f)
    val base = listOf(primary, tertiary, secondary, fourth)
    return HeroPalette(
        base = base,
        highlight = base.map { lerp(it, Color.White, if (isDark) 0.25f else 0.30f) },
        deep = base.map { lerp(it, Color.Black, if (isDark) 0.18f else 0.12f) },
        ambient = lerp(primary, tertiary, 0.5f)
    )
}

/**
 * Four expressive blobs on a single canvas.
 *
 * Idle: they float and breathe. Generating: they speed up, drift, swap sizes and morph between
 * shapes on staggered springs while a shared glow and sonar rings pulse behind them. Success:
 * they pull together, pop back out and release a ring. Submitting or typing gives them a small
 * physical kick ([reactKey] changes trigger it).
 */
@Composable
internal fun FloatingExpressiveShapes(
    isGenerating: Boolean,
    isSuccess: Boolean,
    reactKey: Any? = null,
    modifier: Modifier = Modifier
) {
    val sim = remember { HeroSim() }
    val path = remember { Path() }
    val generating by rememberUpdatedState(isGenerating)
    val succeeded by rememberUpdatedState(isSuccess)

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val palette = remember(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primaryContainer, isDark) {
        buildHeroPalette(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primaryContainer, isDark)
    }

    LaunchedEffect(sim) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
            last = now
            sim.step(dt, generating, succeeded)
        }
    }

    LaunchedEffect(isGenerating) {
        if (isGenerating) sim.nudge(5.5f)
    }

    val firstReact = remember { booleanArrayOf(true) }
    LaunchedEffect(reactKey) {
        if (firstReact[0]) firstReact[0] = false else sim.nudge(1.7f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        drawHero(sim, palette, path, sim.tick.longValue)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun DrawScope.drawHero(sim: HeroSim, palette: HeroPalette, path: Path, frame: Long) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val e = sim.energy.coerceIn(0f, 1f)
    val t = sim.time
    val pull = if (sim.celebrating) sin(PI_F * sim.celebration) else 0f
    val slot = min(w * 0.215f, 84.dp.toPx())
    val baseR = 29.dp.toPx()
    val floatY = 4.dp.toPx()
    val orbitY = 15.dp.toPx()

    // Shared ambient glow
    val glowRadius = w * (0.46f + 0.05f * sin(t * 1.3f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.ambient.copy(alpha = 0.08f + 0.20f * e + 0.22f * pull),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(cx, cy)
    )

    // Sonar rings while generating
    if (e > 0.02f) {
        for (ring in 0..1) {
            val q = (t * 0.32f + ring * 0.5f) % 1f
            drawCircle(
                color = palette.ambient.copy(alpha = (1f - q) * 0.16f * e),
                radius = baseR * 1.4f + q * w * 0.36f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }

    // Pass 1: resolve position, size and rotation
    for (i in 0 until HERO_BLOB_COUNT) {
        val b = sim.blobs[i]
        val phase = t + i * 1.7f
        val spread = 1f - 0.16f * e
        var x = cx + (i - 1.5f) * slot * spread +
            (cos(phase * 0.9f) * 0.8f + sin(phase * 0.47f + i) * 0.5f) * slot * 0.38f * e
        var y = cy + sin(phase * 1.25f) * orbitY * e +
            sin(t * 0.8f + i * 1.57f) * floatY * (1f - 0.5f * e)
        x += (cx - x) * 0.82f * pull
        y += (cy - y) * 0.82f * pull

        val dir = if (i % 2 == 0) 1f else -1f
        val scale = (1f + 0.2f * e * sin(phase * 1.6f + 1.1f) + sim.kick + 0.28f * pull).coerceAtLeast(0.5f)
        b.px = x
        b.py = y
        b.pr = baseR * scale
        b.prot = (b.rotation + sin(t * 0.8f + i * 1.57f) * 6f * (1f - e) + sim.kick * 26f * dir) * DEG_TO_RAD
    }

    // Pass 2: glow halos, drawn first so they never wash over a neighbouring body
    for (i in 0 until HERO_BLOB_COUNT) {
        val b = sim.blobs[i]
        val glowAlpha = 0.30f + 0.35f * e + 0.20f * pull
        val base = palette.base[i]
        val center = Offset(b.px, b.py)
        val radius = b.pr * 1.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    base.copy(alpha = glowAlpha),
                    base.copy(alpha = glowAlpha * 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }

    // Pass 3: bodies
    for (i in 0 until HERO_BLOB_COUNT) {
        val b = sim.blobs[i]
        val phase = t + i * 1.7f
        val cosR = cos(b.prot)
        val sinR = sin(b.prot)
        val wobbleA = 0.03f * e
        val wobbleB = 0.02f * e

        path.rewind()
        for (k in 0 until SHAPE_STEP_COUNT) {
            val angle = k * TWO_PI_F / SHAPE_STEP_COUNT
            var radius = b.from[k] + (b.to[k] - b.from[k]) * b.t
            if (e > 0.01f) {
                radius *= 1f + wobbleA * sin(3f * angle + phase * 2.2f) + wobbleB * sin(2f * angle - phase * 1.7f)
            }
            radius = radius.coerceAtLeast(0.35f) * b.pr
            val ox = COS_TABLE[k] * radius
            val oy = SIN_TABLE[k] * radius
            val px = b.px + ox * cosR - oy * sinR
            val py = b.py + ox * sinR + oy * cosR
            if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        val topLeft = Offset(b.px - b.pr, b.py - b.pr)
        val bottomRight = Offset(b.px + b.pr, b.py + b.pr)

        translate(top = 5.dp.toPx() * (0.8f + 0.4f * e)) {
            drawPath(path, palette.deep[i].copy(alpha = 0.22f))
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(palette.highlight[i], palette.base[i], palette.deep[i]),
                start = topLeft,
                end = bottomRight
            )
        )
        val sheenCenter = Offset(b.px - b.pr * 0.3f, b.py - b.pr * 0.38f)
        clipPath(path) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                    center = sheenCenter,
                    radius = b.pr * 0.85f
                ),
                radius = b.pr * 0.85f,
                center = sheenCenter
            )
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.14f)
                ),
                start = topLeft,
                end = bottomRight
            ),
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Success ring
    if (sim.celebrating) {
        val q = sim.celebration
        val eased = 1f - (1f - q) * (1f - q) * (1f - q)
        drawCircle(
            color = palette.ambient.copy(alpha = (1f - q) * 0.5f),
            radius = baseR * 1.2f + eased * w * 0.42f,
            center = Offset(cx, cy),
            style = Stroke(width = (4f * (1f - q) + 0.5f).dp.toPx())
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Progress view
// ---------------------------------------------------------------------------------------------

private val GENERATION_PHASES = listOf(
    "Tuning to your taste & listening patterns…",
    "Scanning local & cloud tracks for matching vibes…",
    "Discovering fresh recommendations & flow…",
    "Harmonizing energy transitions & playlist order…",
    "Synthesizing smart titles & final touches…"
)

@Composable
internal fun AiPlaylistCreationAnimationView(
    prompt: String,
    status: String?,
    isSuccess: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onMinimize: () -> Unit
) {
    var phaseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isSuccess, error) {
        if (!isSuccess && error == null) {
            while (true) {
                kotlinx.coroutines.delay(2200)
                phaseIndex = (phaseIndex + 1) % GENERATION_PHASES.size
            }
        }
    }

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 2.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = isSuccess,
            transitionSpec = {
                (fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 3 }) togetherWith
                    (fadeOut(tween(140)) + slideOutVertically(tween(160)) { -it / 3 })
            },
            label = "ai_generation_headline"
        ) { done ->
            Text(
                text = if (done) "Your mix is ready" else "Creating your AI Playlist…",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = if (isSuccess) "Opening your new playlist…" else status ?: GENERATION_PHASES[phaseIndex],
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "ai_generation_phase_text"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(320, delayMillis = 100)) +
                slideInVertically(tween(420, delayMillis = 100, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            Surface(
                shape = AbsoluteSmoothCornerShape(28.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prompt.ifBlank { "Crafting your mix…" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                    AiProgressActionButton(isSuccess = isSuccess)
                }
            }
        }

        AnimatedVisibility(
            visible = !isSuccess && error.isNullOrBlank(),
            enter = fadeIn(tween(300, delayMillis = 220)),
            exit = fadeOut(tween(120))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Keep creating in background")
                }
                Text(
                    text = "We'll notify you when it's ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

/** Primary action button of the prompt bar: breathing equalizer while working, a check when done. */
@Composable
private fun AiProgressActionButton(isSuccess: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "ai_progress_button")
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_progress_breathe"
    )
    val corner by animateDpAsState(
        targetValue = if (isSuccess) 27.dp else 20.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "ai_progress_corner"
    )

    Surface(
        shape = AbsoluteSmoothCornerShape(corner, 60),
        color = scheme.primary,
        contentColor = scheme.onPrimary,
        modifier = Modifier
            .size(width = 64.dp, height = 54.dp)
            .graphicsLayer {
                val s = 1f + 0.035f * breathe
                scaleX = s
                scaleY = s
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isSuccess,
                transitionSpec = {
                    (fadeIn(tween(160)) +
                        scaleIn(
                            initialScale = 0.5f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                        )) togetherWith (fadeOut(tween(100)) + scaleOut(targetScale = 0.5f))
                },
                label = "ai_progress_button_content"
            ) { done ->
                if (done) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    AiEqualizerBars(color = LocalContentColor.current)
                }
            }
        }
    }
}

@Composable
private fun AiEqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "ai_equalizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI_F,
        animationSpec = infiniteRepeatable(animation = tween(1300, easing = LinearEasing)),
        label = "ai_equalizer_phase"
    )

    Canvas(modifier = Modifier.size(width = 30.dp, height = 28.dp)) {
        val bars = 5
        val barWidth = 4.dp.toPx()
        val gap = (size.width - barWidth * bars) / (bars - 1)
        for (i in 0 until bars) {
            val level = (sin(phase + i * 1.3f) * 0.6f + sin(phase * 2f + i * 0.7f) * 0.4f + 1f) / 2f
            val barHeight = size.height * (0.26f + 0.74f * level)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
