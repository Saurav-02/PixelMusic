package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.backup.model.BackupSection
import com.saurav.pixelmusic.data.backup.model.ModuleRestoreDetail
import com.saurav.pixelmusic.data.backup.model.RestorePlan
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun BackupModuleSelectionDialog(
    plan: RestorePlan,
    inProgress: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSelectionChanged: (Set<BackupSection>) -> Unit,
    onConfirm: () -> Unit
) {
    val listState = rememberLazyListState()
    val modulesList = remember(plan.availableModules) { plan.availableModules.toList() }
    val selectedCount = plan.selectedModules.size
    val availableCount = plan.availableModules.size
    val dateText = remember(plan.manifest.createdAt) {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(Date(plan.manifest.createdAt))
    }
    val transitionState = remember { MutableTransitionState(false) }
    var shouldShowDialog by remember { mutableStateOf(true) }
    var onDialogHiddenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var initialOpenComplete by remember { mutableStateOf(false) }

    transitionState.targetState = shouldShowDialog

    LaunchedEffect(Unit) {
        delay(320)
        initialOpenComplete = true
    }

    fun closeDialog(afterClose: () -> Unit) {
        if (!shouldShowDialog) return
        onDialogHiddenAction = afterClose
        shouldShowDialog = false
    }

    LaunchedEffect(transitionState.currentState, transitionState.targetState) {
        if (!transitionState.currentState && !transitionState.targetState) {
            onDialogHiddenAction?.let { action ->
                onDialogHiddenAction = null
                action()
            }
        }
    }

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = { closeDialog(onDismiss) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = slideInVertically(
                    initialOffsetY = { it / 10 },
                    animationSpec = tween(360, easing = AiSmoothMorphEasing)
                ) + fadeIn(tween(260)) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(340, easing = AiSmoothMorphEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it / 10 },
                    animationSpec = tween(240, easing = AiSmoothMorphEasing)
                ) + fadeOut(tween(200)) + scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(240)
                ),
                label = "import_module_selection_dialog"
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentWindowInsets = WindowInsets.systemBars,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.presentation_batch_g_backup_restore_modules),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 24.sp,
                                            textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                                        ),
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                navigationIcon = {
                                    FilledIconButton(
                                        modifier = Modifier.padding(start = 6.dp),
                                        onClick = { closeDialog(onBack) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = stringResource(R.string.auth_cd_back)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        },
                        bottomBar = {
                            BottomAppBar(
                                windowInsets = WindowInsets.navigationBars,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledIconButton(
                                            onClick = { onSelectionChanged(plan.availableModules) },
                                            enabled = !inProgress,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.round_select_all_24),
                                                contentDescription = stringResource(R.string.presentation_batch_g_backup_cd_select_all)
                                            )
                                        }
                                        FilledIconButton(
                                            onClick = { onSelectionChanged(emptySet()) },
                                            enabled = !inProgress,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.baseline_deselect_24),
                                                contentDescription = stringResource(R.string.presentation_batch_g_backup_cd_clear_selection)
                                            )
                                        }
                                    }

                                    ExtendedFloatingActionButton(
                                        onClick = { closeDialog(onConfirm) },
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .height(48.dp),
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        if (inProgress) {
                                            LoadingIndicator(modifier = Modifier.height(20.dp))
                                            Spacer(modifier = Modifier.size(8.dp))
                                            Text(
                                                text = stringResource(R.string.presentation_batch_g_backup_restoring),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.rounded_restore_24),
                                                    contentDescription = null
                                                )
                                                Text(
                                                    text = stringResource(R.string.presentation_batch_g_backup_restore_selected),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            item(key = "backup_details_header") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.presentation_batch_g_backup_details),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = GoogleSansRounded,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = stringResource(R.string.presentation_batch_g_backup_created),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = dateText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = stringResource(R.string.presentation_batch_g_backup_app_version),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = plan.manifest.appVersion.ifEmpty { stringResource(R.string.presentation_batch_g_backup_unknown) },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = stringResource(R.string.presentation_batch_g_backup_schema),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.backup_manifest_schema_v,
                                                        plan.manifest.schemaVersion
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (plan.manifest.deviceInfo.model.isNotBlank()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        text = stringResource(R.string.presentation_batch_g_backup_device),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = stringResource(
                                                            R.string.backup_manifest_device_line,
                                                            plan.manifest.deviceInfo.manufacturer,
                                                            plan.manifest.deviceInfo.model
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        AnimatedContent(
                                            targetState = selectedCount,
                                            transitionSpec = {
                                                (fadeIn(tween(160)) + slideInVertically { it / 2 })
                                                    .togetherWith(fadeOut(tween(120)) + slideOutVertically { -it / 2 })
                                            },
                                            label = "restore_selected_count"
                                        ) { count ->
                                            Text(
                                                text = stringResource(R.string.presentation_batch_g_backup_modules_selected, count, availableCount),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = GoogleSansRounded,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (inProgress) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                LoadingIndicator(modifier = Modifier.height(24.dp))
                                                Text(
                                                    text = stringResource(R.string.presentation_batch_g_backup_transfer_progress),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (plan.warnings.isNotEmpty()) {
                                itemsIndexed(plan.warnings, key = { idx, _ -> "warning_$idx" }) { _, warning ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = warning,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }

                            itemsIndexed(modulesList, key = { _, it -> it.key }) { index, section ->
                                val isSelected = section in plan.selectedModules
                                val detail = plan.moduleDetails[section]
                                val entrance = remember {
                                    Animatable(if (initialOpenComplete) 1f else 0f)
                                }
                                LaunchedEffect(Unit) {
                                    if (entrance.value < 1f) {
                                        delay(30L + (index.coerceAtMost(7) * 30L))
                                        entrance.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.84f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                                BackupSectionSelectableCardShared(
                                    section = section,
                                    selected = isSelected,
                                    enabled = !inProgress,
                                    isFirst = index == 0,
                                    isLast = index == modulesList.lastIndex,
                                    detail = detail,
                                    modifier = Modifier
                                        .animateItem()
                                        .expressiveListItemScrollAnimation(listState)
                                        .graphicsLayer {
                                            val p = entrance.value
                                            alpha = p.coerceIn(0f, 1f)
                                            translationY = (1f - p) * 24f
                                        },
                                    onToggle = {
                                        onSelectionChanged(
                                            if (isSelected) {
                                                plan.selectedModules - section
                                            } else {
                                                plan.selectedModules + section
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupSectionSelectableCardShared(
    section: BackupSection,
    selected: Boolean,
    enabled: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    detail: ModuleRestoreDetail? = null,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "backup_card_press_scale"
    )
    val innerCorner by animateDpAsState(
        targetValue = if (selected) 16.dp else 4.dp,
        animationSpec = tween(260, easing = AiSmoothMorphEasing),
        label = "backup_card_inner_corner"
    )
    val topRadius = if (isFirst) 24.dp else innerCorner
    val bottomRadius = if (isLast) 24.dp else innerCorner

    val cardColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(240, easing = AiSmoothMorphEasing),
        label = "backup_card_bg"
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(220, easing = AiSmoothMorphEasing),
        label = "backup_section_icon_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220, easing = AiSmoothMorphEasing),
        label = "backup_section_icon_tint"
    )
    val iconCorner by animateDpAsState(
        targetValue = if (selected) 24.dp else 14.dp,
        animationSpec = tween(260, easing = AiSmoothMorphEasing),
        label = "backup_icon_corner"
    )

    Surface(
        onClick = onToggle,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius
        ),
        color = cardColor,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = iconContainerColor,
                shape = RoundedCornerShape(iconCorner),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(section.iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansRounded
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = section.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (detail != null && detail.entryCount > 0) {
                    Text(
                        text = stringResource(R.string.presentation_batch_g_backup_entries_will_replace, detail.entryCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Switch(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = enabled,
                thumbContent = {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                        label = "switch_thumb_icon"
                    ) { isSelected ->
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
