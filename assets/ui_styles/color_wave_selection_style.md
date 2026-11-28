# Material You Expressive Color Wave & Corner-Morph Selection Style

Saved reference for PixelMusic's signature touch-point radial color wave and corner-only expressive morphing effect.

## Visual Specification
- **Easing**: `CubicBezierEasing(0.32f, 0.0f, 0.18f, 1.0f)` (`ExpressiveWaveEasing`)
- **Selection wave duration**: `600–650ms`
- **Tap pulse wave duration**: `520–560ms`
- **Corner morph (grouped selectors)**:
  - Unselected: `24.dp` outer corners, `6.dp` inner corners
  - Selected: `28.dp` pill corners on all sides
  - **Zero** vertical shift, padding change, scaling, or spring bounce (layout stays 100% still)
- **Color wave**:
  - Captures exact local touch point (`PressInteraction.Press` or `awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)`)
  - Draws a soft-edged `Brush.radialGradient` expanding from the touch point with `0.38f–0.40f` feathering so the wave glides across the pill/card like liquid color.

## Reusable Compose Modifiers
Located at `app/src/main/java/com/saurav/pixelmusic/presentation/components/ExpressiveWaveEffect.kt`:

1. **Tap Color Wave (Buttons, Pills, Search Bars, Song Cards, Mini Player, Mood Cards)**
   ```kotlin
   Modifier.expressiveTapColorWave(
       waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
       shape = RoundedCornerShape(28.dp),
       durationMillis = 560
   )
   ```

2. **Selection Color Wave (Library Tab Pills, Explore Filter Chips, Preference Selectors)**
   ```kotlin
   Modifier.expressiveSelectionColorWave(
       isSelected = isSelected,
       unselectedColor = MaterialTheme.colorScheme.surfaceContainerHigh,
       selectedColor = MaterialTheme.colorScheme.primary,
       shape = CircleShape,
       durationMillis = 600
   )
   ```
