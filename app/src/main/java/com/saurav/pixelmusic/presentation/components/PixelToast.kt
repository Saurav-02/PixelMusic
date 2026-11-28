package com.saurav.pixelmusic.presentation.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PixelToastType {
    INFO,
    SUCCESS,
    ERROR,
    QUEUE,
    FAVORITE,
    DOWNLOAD,
    PLAYLIST,
    LYRICS
}

data class PixelToastMessage(
    val id: Long,
    val message: String,
    val type: PixelToastType,
    val durationMs: Long
)

/**
 * Global 3-tier Material You Expressive toast dispatcher for PixelMusic:
 * 1. In-app: rendered by [PixelToastHost] mounted at the root of MainActivity with full dynamic theme.
 * 2. Background + Overlay permission granted: non-focusable WindowManager TYPE_APPLICATION_OVERLAY pill.
 * 3. Background + No overlay permission: safe fallback to system [Toast].
 */
object PixelToast {
    const val LENGTH_SHORT: Int = Toast.LENGTH_SHORT
    const val LENGTH_LONG: Int = Toast.LENGTH_LONG

    class Handle internal constructor(
        private val context: Context?,
        private val message: String,
        private val long: Boolean
    ) {
        fun show() {
            PixelToast.show(context, message, long = long)
        }
    }

    fun makeText(context: Context?, text: CharSequence?, duration: Int): Handle =
        Handle(context, text?.toString().orEmpty(), duration == LENGTH_LONG)

    fun makeText(context: Context, resId: Int, duration: Int): Handle =
        Handle(context, context.getString(resId), duration == LENGTH_LONG)

    private val idCounter = AtomicLong(0L)
    private val hostIdCounter = AtomicLong(0L)
    private val attachedHostCount = AtomicInteger(0)
    internal val activeHostIds = mutableStateListOf<Long>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _currentToast = MutableStateFlow<PixelToastMessage?>(null)
    val currentToast: StateFlow<PixelToastMessage?> = _currentToast.asStateFlow()

    @Volatile
    private var activeOverlayView: View? = null
    @Volatile
    private var activeOverlayDismissRunnable: Runnable? = null

    internal fun allocateHostId(): Long = hostIdCounter.incrementAndGet()

    internal fun registerHost(hostId: Long) {
        if (!activeHostIds.contains(hostId)) {
            activeHostIds.add(hostId)
            attachedHostCount.incrementAndGet()
        }
    }

    internal fun unregisterHost(hostId: Long) {
        if (activeHostIds.remove(hostId)) {
            attachedHostCount.decrementAndGet()
        }
    }

    fun dismissCurrent() {
        _currentToast.value = null
    }

    fun show(
        context: Context?,
        message: String,
        long: Boolean = false,
        type: PixelToastType? = null
    ) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return

        val resolvedType = type ?: inferToastType(trimmed)
        val durationMs = if (long) 3600L else 2400L

        if (attachedHostCount.get() > 0) {
            _currentToast.value = PixelToastMessage(
                id = idCounter.incrementAndGet(),
                message = trimmed,
                type = resolvedType,
                durationMs = durationMs
            )
            return
        }

        val appContext = context?.applicationContext ?: return
        val canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(appContext)
        if (canOverlay) {
            mainHandler.post {
                showWindowOverlayToast(appContext, trimmed, resolvedType, durationMs)
            }
        } else {
            mainHandler.post {
                Toast.makeText(
                    appContext,
                    trimmed,
                    if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun show(
        context: Context,
        messageResId: Int,
        long: Boolean = false,
        type: PixelToastType? = null
    ) {
        show(context, context.getString(messageResId), long, type)
    }

    private fun inferToastType(message: String): PixelToastType {
        val m = message.lowercase()
        return when {
            m.contains("fail") || m.contains("error") || m.contains("couldn't") ||
                m.contains("could not") || m.contains("unable") || m.contains("not available") ||
                m.contains("requires") || m.contains("login") || m.contains("empty") ||
                m.contains("maximum") || m.contains("not initialized") || m.contains("invalid") ||
                m.contains("no playable") || m.contains("first") -> PixelToastType.ERROR

            m.contains("queue") || m.contains("play next") || m.contains("playing next") -> PixelToastType.QUEUE

            m.contains("like") || m.contains("favorite") || m.contains("favourite") ||
                m.contains("interested") -> PixelToastType.FAVORITE

            m.contains("download") || m.contains("saved") || m.contains("export") ||
                m.contains("backup") || m.contains("storage") -> PixelToastType.DOWNLOAD

            m.contains("lyric") -> PixelToastType.LYRICS

            m.contains("playlist") || m.contains("library") || m.contains("sync") ||
                m.contains("import") || m.contains("rescan") || m.contains("mix") -> PixelToastType.PLAYLIST

            m.contains("copied") || m.contains("cleared") || m.contains("reset") ||
                m.contains("created") || m.contains("updated") || m.contains("success") ||
                m.contains("up to date") || m.contains("done") || m.contains("applied") -> PixelToastType.SUCCESS

            else -> PixelToastType.INFO
        }
    }

    private fun showWindowOverlayToast(
        appContext: Context,
        message: String,
        type: PixelToastType,
        durationMs: Long
    ) {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        removeActiveOverlay(wm)

        val isDark = (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val density = appContext.resources.displayMetrics.density
        fun dp(v: Float): Int = (v * density).roundToInt()

        val pillBg = resolveOverlayColor(
            appContext,
            darkSystemRes = android.R.color.system_neutral1_800,
            lightSystemRes = android.R.color.system_neutral1_50,
            darkFallback = 0xFF231F1E.toInt(),
            lightFallback = 0xFFFDF2EC.toInt(),
            isDark = isDark
        )
        val badgeBg = when (type) {
            PixelToastType.ERROR -> if (isDark) 0xFF690005.toInt() else 0xFFFFDAD6.toInt()
            else -> resolveOverlayColor(
                appContext,
                darkSystemRes = android.R.color.system_accent1_700,
                lightSystemRes = android.R.color.system_accent1_100,
                darkFallback = 0xFF5D3A29.toInt(),
                lightFallback = 0xFFFFDCCB.toInt(),
                isDark = isDark
            )
        }
        val iconTint = when (type) {
            PixelToastType.ERROR -> if (isDark) 0xFFFFDAD6.toInt() else 0xFF410002.toInt()
            else -> resolveOverlayColor(
                appContext,
                darkSystemRes = android.R.color.system_accent1_100,
                lightSystemRes = android.R.color.system_accent1_800,
                darkFallback = 0xFFFFDCCB.toInt(),
                lightFallback = 0xFF4A2512.toInt(),
                isDark = isDark
            )
        }
        val textColor = if (isDark) 0xFFF5ECE8.toInt() else 0xFF201A17.toInt()

        val root = FrameLayout(appContext).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(dp(20f), dp(12f), dp(20f), dp(12f))
        }

        val pill = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(8f), dp(18f), dp(8f))
            elevation = dp(8f).toFloat()
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(999f).toFloat()
                setColor(pillBg)
            }
        }

        val badgeContainer = FrameLayout(appContext).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36f), dp(36f))
        }

        val cookieView = ExpressiveOverlayCookieView(appContext, badgeBg, lobesFor(type)).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        badgeContainer.addView(cookieView)

        val iconView = ImageView(appContext).apply {
            layoutParams = FrameLayout.LayoutParams(dp(19f), dp(19f), Gravity.CENTER)
            setImageResource(drawableForOverlay(type))
            setColorFilter(iconTint)
        }
        badgeContainer.addView(iconView)
        pill.addView(badgeContainer)

        val textView = TextView(appContext).apply {
            text = message
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(260f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(12f)
            }
        }
        pill.addView(textView)
        root.addView(
            pill,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(48f)
        }

        try {
            pill.alpha = 0f
            pill.scaleX = 0.62f
            pill.scaleY = 0.62f
            pill.translationY = dp(64f).toFloat()
            wm.addView(root, params)
            activeOverlayView = root

            val spinAnimator = ObjectAnimator.ofFloat(cookieView, View.ROTATION, 0f, 360f).apply {
                duration = 14_000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }

            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(pill, View.TRANSLATION_Y, dp(64f).toFloat(), 0f).apply {
                        duration = 460L
                        interpolator = OvershootInterpolator(1.15f)
                    },
                    ObjectAnimator.ofFloat(pill, View.SCALE_X, 0.62f, 1f).apply {
                        duration = 460L
                        interpolator = OvershootInterpolator(1.1f)
                    },
                    ObjectAnimator.ofFloat(pill, View.SCALE_Y, 0.62f, 1f).apply {
                        duration = 460L
                        interpolator = OvershootInterpolator(1.1f)
                    },
                    ObjectAnimator.ofFloat(pill, View.ALPHA, 0f, 1f).apply {
                        duration = 220L
                        interpolator = DecelerateInterpolator()
                    }
                )
                start()
            }

            val dismissRunnable = Runnable {
                if (activeOverlayView !== root) {
                    spinAnimator.cancel()
                    return@Runnable
                }
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(pill, View.TRANSLATION_Y, 0f, dp(44f).toFloat()),
                        ObjectAnimator.ofFloat(pill, View.SCALE_X, 1f, 0.80f),
                        ObjectAnimator.ofFloat(pill, View.SCALE_Y, 1f, 0.80f),
                        ObjectAnimator.ofFloat(pill, View.ALPHA, 1f, 0f)
                    )
                    duration = 250L
                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            spinAnimator.cancel()
                            if (activeOverlayView === root) {
                                removeActiveOverlay(wm)
                            }
                        }
                    })
                    start()
                }
            }
            activeOverlayDismissRunnable = dismissRunnable
            mainHandler.postDelayed(dismissRunnable, durationMs)
        } catch (_: Throwable) {
            Toast.makeText(
                appContext,
                message,
                if (durationMs > 3000L) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun removeActiveOverlay(wm: WindowManager) {
        activeOverlayDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        activeOverlayDismissRunnable = null
        activeOverlayView?.let { view ->
            runCatching { wm.removeViewImmediate(view) }
        }
        activeOverlayView = null
    }

    private fun resolveOverlayColor(
        context: Context,
        darkSystemRes: Int,
        lightSystemRes: Int,
        darkFallback: Int,
        lightFallback: Int,
        isDark: Boolean
    ): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val res = if (isDark) darkSystemRes else lightSystemRes
            runCatching { return ContextCompat.getColor(context, res) }
        }
        return if (isDark) darkFallback else lightFallback
    }

    private fun lobesFor(type: PixelToastType): Int = when (type) {
        PixelToastType.ERROR -> 4
        PixelToastType.QUEUE -> 6
        PixelToastType.FAVORITE -> 5
        PixelToastType.DOWNLOAD -> 12
        else -> 8
    }

    private fun drawableForOverlay(type: PixelToastType): Int = when (type) {
        PixelToastType.QUEUE -> R.drawable.rounded_queue_music_24
        PixelToastType.FAVORITE -> R.drawable.round_favorite_24
        PixelToastType.DOWNLOAD -> R.drawable.rounded_download_24
        PixelToastType.PLAYLIST -> R.drawable.rounded_playlist_add_24
        PixelToastType.LYRICS -> R.drawable.rounded_lyrics_24
        else -> R.drawable.rounded_music_note_24
    }
}

private class ExpressiveOverlayCookieView(
    context: Context,
    fillColor: Int,
    private val lobes: Int
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    private val path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        val cx = w * 0.5f
        val cy = h * 0.5f
        val baseR = minOf(cx, cy) * 0.90f
        val amp = if (lobes >= 10) 0.045f else 0.068f
        val steps = 120
        for (i in 0..steps) {
            val theta = (2.0 * PI * i) / steps
            val r = baseR * (1.0 + amp * cos(lobes * theta)).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }
}

/**
 * Compose host for Material You Expressive toast pills.
 * Mounted at the root of MainActivity and inside [SmoothModalBottomSheet] dialog windows.
 * Uses a topmost-host stack so only the highest active window renders the toast without duplicates.
 */
@Composable
fun PixelToastHost(
    modifier: Modifier = Modifier,
    usePopup: Boolean = false
) {
    val hostId = remember { PixelToast.allocateHostId() }
    DisposableEffect(hostId) {
        PixelToast.registerHost(hostId)
        onDispose {
            PixelToast.unregisterHost(hostId)
        }
    }

    val isTopmostHost = PixelToast.activeHostIds.lastOrNull() == hostId
    val activeMessage by PixelToast.currentToast.collectAsState()

    if (!isTopmostHost) return

    if (usePopup) {
        val current = activeMessage ?: return
        val density = LocalDensity.current
        val bottomMarginPx = remember(density) { with(density) { 40.dp.roundToPx() } }
        val positionProvider = remember(bottomMarginPx) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
                    val y = (windowSize.height - popupContentSize.height - bottomMarginPx).coerceAtLeast(0)
                    return IntOffset(x, y)
                }
            }
        }

        Popup(
            popupPositionProvider = positionProvider,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                clippingEnabled = true
            )
        ) {
            Box(
                modifier = modifier
                    .zIndex(Float.MAX_VALUE)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                key(current.id) {
                    PixelToastPill(
                        toast = current,
                        onFinished = {
                            if (PixelToast.currentToast.value?.id == current.id) {
                                PixelToast.dismissCurrent()
                            }
                        }
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .zIndex(Float.MAX_VALUE)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 4.dp)
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val current = activeMessage
            if (current != null) {
                key(current.id) {
                    PixelToastPill(
                        toast = current,
                        onFinished = {
                            if (PixelToast.currentToast.value?.id == current.id) {
                                PixelToast.dismissCurrent()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelToastPill(
    toast: PixelToastMessage,
    onFinished: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val startOffsetYPx = remember(density) { with(density) { 84.dp.toPx() } }
    val exitOffsetYPx = remember(density) { with(density) { 52.dp.toPx() } }

    val offsetY = remember { Animatable(startOffsetYPx) }
    val pillScale = remember { Animatable(0.56f) }
    val pillAlpha = remember { Animatable(0f) }
    val badgePopScale = remember { Animatable(0.55f) }
    var isDismissing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pixelToastSpin")
    val shapeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pixelToastShapeRotation"
    )

    suspend fun animateOutAndFinish() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            offsetY.animateTo(
                targetValue = exitOffsetYPx,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
        }
        scope.launch {
            pillScale.animateTo(
                targetValue = 0.78f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
        }
        pillAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    LaunchedEffect(toast.id) {
        // MusicRecognitionOverlay-inspired lush spring rise & expand from bottom
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f)
            )
        }
        launch {
            pillScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.70f, stiffness = 250f)
            )
        }
        launch {
            pillAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(50L)
            badgePopScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.56f, stiffness = 320f)
            )
        }

        delay(toast.durationMs)
        animateOutAndFinish()
    }

    val shapeType = remember(toast.type) {
        when (toast.type) {
            PixelToastType.ERROR -> AiExpressiveShapeType.FOUR_HEAD_COOKIE
            PixelToastType.QUEUE -> AiExpressiveShapeType.SIX_COOKIE
            PixelToastType.FAVORITE -> AiExpressiveShapeType.FIVE_COOKIE
            PixelToastType.DOWNLOAD -> AiExpressiveShapeType.SUNNY_COOKIE
            PixelToastType.LYRICS -> AiExpressiveShapeType.ROUNDED_SQUARE
            PixelToastType.PLAYLIST -> AiExpressiveShapeType.COOKIE
            PixelToastType.SUCCESS -> AiExpressiveShapeType.COOKIE
            PixelToastType.INFO -> AiExpressiveShapeType.COOKIE
        }
    }

    val (badgeColor, iconColor) = when (toast.type) {
        PixelToastType.ERROR -> colorScheme.errorContainer to colorScheme.onErrorContainer
        PixelToastType.QUEUE -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        PixelToastType.FAVORITE -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        PixelToastType.DOWNLOAD -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        PixelToastType.LYRICS -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        PixelToastType.PLAYLIST -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        PixelToastType.SUCCESS -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        PixelToastType.INFO -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
    }

    val pillContainerColor = colorScheme.surfaceContainerHigh
    val pillTextColor = colorScheme.onSurface

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = offsetY.value
                scaleX = pillScale.value
                scaleY = pillScale.value
                alpha = pillAlpha.value
                clip = false
            }
            .padding(16.dp)
            .pointerInput(toast.id) {
                var draggedY = 0f
                detectVerticalDragGestures(
                    onDragStart = { draggedY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0f) draggedY += dragAmount
                    },
                    onDragEnd = {
                        if (draggedY > 32f && !isDismissing) {
                            scope.launch { animateOutAndFinish() }
                        }
                    }
                )
            }
    ) {
        Surface(
            shape = CircleShape,
            color = pillContainerColor,
            tonalElevation = 4.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        val s = badgePopScale.value
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center
            ) {
                // Rotating Material You Expressive shape behind the stationary crisp icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = shapeRotation
                        }
                        .drawAiMorphShape(
                            from = shapeType,
                            to = shapeType,
                            morphProgress = { 1f },
                            brush = SolidColor(badgeColor)
                        )
                )

                PixelToastIcon(
                    type = toast.type,
                    tint = iconColor
                )
            }

            Text(
                text = toast.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                ),
                color = pillTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        }
    }
}

@Composable
private fun PixelToastIcon(
    type: PixelToastType,
    tint: Color
) {
    val vectorIcon: ImageVector? = when (type) {
        PixelToastType.SUCCESS -> Icons.Rounded.CheckCircle
        PixelToastType.ERROR -> Icons.Rounded.Warning
        PixelToastType.INFO -> Icons.Rounded.Info
        else -> null
    }
    if (vectorIcon != null) {
        Icon(
            imageVector = vectorIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    } else {
        val resId = when (type) {
            PixelToastType.QUEUE -> R.drawable.rounded_queue_music_24
            PixelToastType.FAVORITE -> R.drawable.round_favorite_24
            PixelToastType.DOWNLOAD -> R.drawable.rounded_download_24
            PixelToastType.PLAYLIST -> R.drawable.rounded_playlist_add_24
            PixelToastType.LYRICS -> R.drawable.rounded_lyrics_24
            else -> R.drawable.rounded_music_note_24
        }
        Icon(
            painter = painterResource(resId),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}
