@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.saurav.pixelmusic.presentation.components.lyrics

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.lyrics.PixelMusicLyricsEngine
import com.saurav.pixelmusic.lyrics.PixelMusicLyricsPreferences
import com.saurav.pixelmusic.presentation.components.SmoothModalBottomSheet
import com.saurav.pixelmusic.presentation.utils.LocalAppHapticsConfig
import com.saurav.pixelmusic.presentation.utils.performAppCompatHapticFeedback
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Drag-to-reorder bottom sheet for lyrics provider priority.
 * Providers are non-selectable and tried from top to bottom when fetching lyrics.
 */
@Composable
fun LyricsProvidersReorderSheet(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current

    val savedOrder by remember(context) { PixelMusicLyricsPreferences.providerOrderFlow(context) }
        .collectAsStateWithLifecycle(initialValue = PixelMusicLyricsPreferences.DefaultProviderOrder)
    val orderOverride by remember(context) { PixelMusicLyricsPreferences.orderOverrideFlow(context) }
        .collectAsStateWithLifecycle(initialValue = false)

    var localOrder by remember { mutableStateOf(savedOrder) }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            localOrder = localOrder.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            performAppCompatHapticFeedback(
                view,
                appHapticsConfig,
                HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            )
        }
    )

    LaunchedEffect(savedOrder) {
        if (!reorderableState.isAnyItemDragging) {
            localOrder = savedOrder
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && localOrder != savedOrder) {
            PixelMusicLyricsPreferences.setProviderOrder(context, localOrder)
        }
    }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    SmoothModalBottomSheet(
        onDismissRequest = {
            if (localOrder != savedOrder) {
                scope.launch { PixelMusicLyricsPreferences.setProviderOrder(context, localOrder) }
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = navBottom + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lyrics Providers",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Drag to reorder priority from top to bottom.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        localOrder = PixelMusicLyricsPreferences.DefaultProviderOrder
                        scope.launch {
                            PixelMusicLyricsPreferences.setProviderOrder(
                                context,
                                PixelMusicLyricsPreferences.DefaultProviderOrder
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_restart_alt_24),
                        contentDescription = "Reset default order",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prioritize my order",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Pick your top provider over extra features like duets or word timing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = orderOverride,
                        onCheckedChange = { enabled ->
                            scope.launch { PixelMusicLyricsPreferences.setOrderOverride(context, enabled) }
                        },
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(localOrder, key = { _, providerId -> providerId }) { index, providerId ->
                    ReorderableItem(reorderableState, key = providerId) { isDragging ->
                        LaunchedEffect(isDragging) {
                            if (isDragging) {
                                performAppCompatHapticFeedback(
                                    view,
                                    appHapticsConfig,
                                    HapticFeedbackConstants.GESTURE_START
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDragging) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shadowElevation = if (isDragging) 4.dp else 0.dp,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_drag_indicator_24),
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.draggableHandle(),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = PixelMusicLyricsEngine.getDisplayName(providerId),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (localOrder != savedOrder) {
                        scope.launch { PixelMusicLyricsPreferences.setProviderOrder(context, localOrder) }
                    }
                    onDismiss()
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Done",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
