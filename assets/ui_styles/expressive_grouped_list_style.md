# Material You Expressive Grouped List Card Style

Saved reference for PixelMusic's Material You Expressive grouped list cards (used in Recently Played, Daily Mix, Library Songs, Playlist Detail, Album Detail, Explore "More to explore", etc.).

## Visual Specification
- **Outer corners (top of first item, bottom of last item, or single item)**: `24.dp`
- **Inner corners (adjacent edges between list items)**: `4.dp`
- **Vertical gap between cards**: `2.dp` (`ExpressiveGroupedCardSpacing`)
- **Surface fill**: `MaterialTheme.colorScheme.surfaceContainerLow` (borderless tonal card, no outlines)
- **Scroll entrance**: `.expressiveListItemScrollAnimation(listState)`
- **Tap feedback**: Combine with `.expressiveTapColorWave(waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), shape = cardShape)`

## Reusable Compose Helpers
Located at `app/src/main/java/com/saurav/pixelmusic/presentation/components/ExpressiveGroupedListStyle.kt`:

```kotlin
val cardShape = expressiveGroupedCardShape(index, items.size)

Column(verticalArrangement = Arrangement.spacedBy(ExpressiveGroupedCardSpacing)) {
    items.forEachIndexed { index, item ->
        val shape = expressiveGroupedCardShape(index, items.size)
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .expressiveTapColorWave(
                    waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    shape = shape
                )
        ) {
            // Item content
        }
    }
}
```
