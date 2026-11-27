package com.tensei.ytxtube

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

object GlassmorphicTheme {
    val backgroundStart = Color(0xFF0D0E12)
    val backgroundMiddle = Color(0xFF1B1528)
    val backgroundEnd = Color(0xFF24192C)
    val surface = Color(0x661A1A24)
    val goldStart = Color(0xFFE6C88A)
    val goldMid = Color(0xFF9E7F47)
    val borderBrush = Brush.linearGradient(listOf(goldStart, goldMid, goldStart))
}

@Composable
fun AmbientGlowBackground(modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF08090D))) {
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val radius = maxOf(size.width, size.height) * 0.78f
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0x33D4AF37), Color.Transparent),
                            center = Offset(size.width * 0.16f, size.height * 0.18f),
                            radius = radius,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0x331E3A8A), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.40f),
                            radius = radius,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0x334C1D95), Color.Transparent),
                            center = Offset(size.width * 0.48f, size.height * 0.92f),
                            radius = radius,
                        ),
                    )
                }
                .blur(96.dp),
        )
    }
}

fun Modifier.glassmorphicBackground(): Modifier = drawBehind {
    drawRect(Color(0xFF08090D))
    val radius = maxOf(size.width, size.height) * 0.78f
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x33D4AF37), Color.Transparent),
            center = Offset(size.width * 0.16f, size.height * 0.18f),
            radius = radius,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x331E3A8A), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.40f),
            radius = radius,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x334C1D95), Color.Transparent),
            center = Offset(size.width * 0.48f, size.height * 0.92f),
            radius = radius,
        ),
    )
}

fun Modifier.bubbleGlassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
): Modifier = shadow(
    elevation = 8.dp,
    shape = shape,
    clip = false,
    ambientColor = Color.Black.copy(alpha = 0.28f),
    spotColor = Color.Black.copy(alpha = 0.38f),
).clip(shape)
    .background(Color(0xFF101116).copy(alpha = 0.6f))
    .drawBehind {
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.11f),
                0.24f to GlassmorphicTheme.goldStart.copy(alpha = 0.045f),
                0.62f to Color.Transparent,
                1f to Color.Transparent,
            ),
        )
    }
    .metallicBorder(shape)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState ?: rememberModalBottomSheetState(),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.Transparent,
        contentColor = Color.White,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .bubbleGlassSurface(RoundedCornerShape(24.dp)),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(GlassmorphicTheme.borderBrush),
            )
            content()
        }
    }
}

fun Modifier.metallicBorder(
    shape: Shape,
    width: Dp = 1.dp,
): Modifier = border(width, GlassmorphicTheme.borderBrush, shape)

fun Modifier.glassmorphicSurface(
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    fill: Color = GlassmorphicTheme.surface,
    borderWidth: Dp = 1.dp,
): Modifier = clip(shape)
    .background(fill)
    .drawBehind {
        drawRect(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.04f),
                ),
            ),
        )
    }
    .metallicBorder(shape, borderWidth)

@Composable
fun Modifier.appScreenBackground(): Modifier =
    if (isDarkTheme()) {
        this
    } else {
        background(MaterialTheme.colorScheme.surface)
    }

/**
 * One place to decide whether the refractive glass shaders can run. Old Android
 * versions and low-RAM TVs keep the same glass surfaces through a cheaper
 * translucent fallback instead of losing the design language or stuttering.
 */
object GlassQuality {
    @Volatile
    private var deviceSupports = isRuntimeShaderSupported()

    /**
     * True only when the device can draw the shaders *and* the viewer has kept
     * Liquid Glass switched on. Compose observes this state, so flipping the
     * setting instantly swaps every surface between glass and solid Material.
     */
    var effectsSupported by mutableStateOf(deviceSupports)
        private set

    fun init(context: Context) {
        deviceSupports = isRuntimeShaderSupported()
        effectsSupported = deviceSupports && GlassSettings.enabled 
}
        

    fun setEnabled(enabled: Boolean) {
        effectsSupported = deviceSupports && enabled
    }
}

/**
 * User-facing Liquid Glass preference. Stored in the same "appearance" file as
 * the theme so it survives restarts, but kept separate from the theme mode.
 */
object GlassSettings {
    private const val PREFS = "appearance"
    private const val KEY = "liquid_glass"

    private var prefs: SharedPreferences? = null

    var enabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs = it }
        enabled = p.getBoolean(KEY, true)
        GlassQuality.setEnabled(enabled)
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs?.edit()?.putBoolean(KEY, value)?.apply()
        GlassQuality.setEnabled(value)
    }
}









@Composable
fun glassFill(alpha: Float = 1f): Color {
    if (!GlassQuality.effectsSupported) {
        // Solid Material surface: no translucency, no blur, no backdrop.
        return MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dark = isDarkTheme()
    return if (dark) GlassmorphicTheme.surface.copy(alpha = GlassmorphicTheme.surface.alpha * alpha) else
        Color(0xFFFAFAFA).copy(alpha = 0.22f * alpha)
        
}

/**
 * Opaque fill for controls drawn directly on top of video. Those controls keep
 * white icons in both themes, so with Liquid Glass off they need a dark solid
 * surface rather than the light Material surface used by sheets and dialogs.
 */
@Composable
fun glassOverlayFill(alpha: Float = 1f): Color =
    if (!GlassQuality.effectsSupported) Color(0xFF1C1C1E) else glassFill(alpha)

/** The backdrop captured behind the player, used by in-player liquid glass controls. */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Immutable
data class GlassSpec(
    val shape: Shape,
    val tint: Color,
    val blur: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val shadow: Shadow?,
    val highlight: Highlight?,
    val innerShadow: InnerShadow?,
    val chromaticAberration: Boolean,
)

@Composable
fun glassSpec(
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
    tint: Color = glassFill(),
    blur: Dp = 26.dp,
    refractionHeight: Dp = 20.dp,
    refractionAmount: Dp = 32.dp,
    shadow: Shadow? = Shadow.Default,
    highlight: Highlight? = Highlight.Default,
    innerShadow: InnerShadow? = InnerShadow.Default.copy(radius = 12.dp, alpha = 0.65f),
    chromaticAberration: Boolean = true,
): GlassSpec = GlassSpec(
    shape = shape,
    // With Liquid Glass off every tint becomes a solid Material fill, even when
    // a caller supplied a translucent colour for the shader path.
    tint = if (GlassQuality.effectsSupported) tint else tint.copy(alpha = 1f),
    blur = blur,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    shadow = shadow,
    highlight = highlight,
    innerShadow = innerShadow,
    chromaticAberration = chromaticAberration,
)


fun Modifier.liquidGlass(backdrop: Backdrop, spec: GlassSpec): Modifier =
    if (GlassQuality.effectsSupported) {
        drawBackdrop(
            backdrop = backdrop,
            shape = { spec.shape },
            effects = {
                vibrancy()
                blur(spec.blur.toPx())
                lens(
                    spec.refractionHeight.toPx(),
                    spec.refractionAmount.toPx(),
                    chromaticAberration = spec.chromaticAberration,
                )
            },
            highlight = spec.highlight?.let { h -> { h } },
            shadow = spec.shadow?.let { s -> { s } },
            innerShadow = spec.innerShadow?.let { s -> { s } },
            onDrawSurface = {
                if (spec.tint.isSpecified) drawRect(spec.tint)
            },
        ).metallicBorder(spec.shape)
    } else {
        // Android 12 and older (plus low-end TVs without runtime shaders) cannot
        // render the refractive glass shader. Keep the same frosted surface look
        // with a translucent fill, soft top sheen and hairline edge instead of
        // removing the glass entirely.
        fallbackGlass(spec)
    }

private fun Modifier.fallbackGlass(spec: GlassSpec): Modifier =
    if (!GlassQuality.effectsSupported) {
        // Liquid Glass is switched off: a plain, fully opaque Material surface.
        this
            .clip(spec.shape)
            .background(spec.tint.copy(alpha = 1f))
            .metallicBorder(spec.shape)
    } else {
        // Android 12 and older (plus low-end TVs without runtime shaders) cannot
        // render the refractive glass shader. Keep the same frosted surface look
        // with a translucent fill, soft top sheen and hairline edge instead of
        // removing the glass entirely.
        this.glassmorphicSurface(spec.shape, spec.tint)
    }
