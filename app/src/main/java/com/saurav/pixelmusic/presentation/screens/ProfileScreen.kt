package com.saurav.pixelmusic.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.model.AvatarBackground
import com.saurav.pixelmusic.data.model.AvatarCategory
import com.saurav.pixelmusic.data.model.BuiltInAvatar
import com.saurav.pixelmusic.data.model.ProfileAvatarCatalog
import com.saurav.pixelmusic.presentation.components.CollapsibleCommonTopBar
import com.saurav.pixelmusic.presentation.components.MiniPlayerHeight
import com.saurav.pixelmusic.presentation.components.PixelToast
import com.saurav.pixelmusic.presentation.components.ProfileAvatar
import com.saurav.pixelmusic.presentation.viewmodel.ProfileViewModel
import com.saurav.pixelmusic.ui.blur.LocalScreenGlassState
import com.saurav.pixelmusic.ui.blur.glassRoot
import com.saurav.pixelmusic.ui.blur.rememberGlassState
import com.saurav.pixelmusic.ui.modifiers.scrollMotionBlur
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentBuiltIn = remember(uiState.profile.avatarSpec) {
        ProfileAvatarCatalog.parseBuiltIn(uiState.profile.avatarSpec)
    }

    var selectedBgId by remember(currentBuiltIn?.second?.id) {
        mutableStateOf(currentBuiltIn?.second?.id ?: ProfileAvatarCatalog.backgrounds.first().id)
    }
    var selectedCategory by remember { mutableStateOf(AvatarCategory.ALL) }
    var nameDraft by remember { mutableStateOf("") }
    var hasInitializedName by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.profile.displayName, uiState.ytUsername) {
        if (!hasInitializedName) {
            val initial = uiState.profile.displayName.ifBlank { uiState.ytUsername }
            if (initial.isNotBlank() || uiState.profile.updatedAt > 0L) {
                nameDraft = initial
                hasInitializedName = true
            }
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        PixelToast.show(context, msg)
        viewModel.consumeStatusMessage()
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.setPickedPhoto(uri)
    }

    val fallbackPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.setPickedPhoto(uri)
    }

    fun launchPhotoPicker() {
        runCatching {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }.onFailure {
            runCatching { fallbackPhotoLauncher.launch("image/*") }
        }
    }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { transitionState.targetState = true }
    val enterTransition = rememberTransition(transitionState, label = "ProfileAppear")
    val contentAlpha by enterTransition.animateFloat(
        label = "ProfileContentAlpha",
        transitionSpec = { tween(durationMillis = 360, easing = FastOutSlowInEasing) }
    ) { if (it) 1f else 0f }
    val contentOffset by enterTransition.animateDp(
        label = "ProfileContentOffset",
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow) }
    ) { if (it) 0.dp else 28.dp }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0
                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }
                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight
                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }
                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    val selectedBackground = remember(selectedBgId) {
        ProfileAvatarCatalog.backgrounds.firstOrNull { it.id == selectedBgId }
            ?: ProfileAvatarCatalog.backgrounds.first()
    }
    val previewName = nameDraft.trim().ifBlank { uiState.effectiveDisplayName.ifBlank { "Guest User" } }
    val hasGooglePhoto = uiState.ytAvatarUrl.isNotBlank()
    val isUsingGooglePhoto = hasGooglePhoto && uiState.profile.avatarSpec.isBlank()
    val isUsingInitial = uiState.effectiveLocalAvatar.isBlank()
    val nameHasUnsavedChanges = nameDraft.trim() != uiState.profile.displayName.trim()

    val ringGradient = Brush.sweepGradient(
        colors = listOf(
            colors.primary,
            colors.tertiary,
            colors.secondary,
            colors.primaryContainer,
            colors.primary
        )
    )

    val profileGlassState = rememberGlassState()
    CompositionLocalProvider(LocalScreenGlassState provides profileGlassState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = currentTopBarHeightDp + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .glassRoot(profileGlassState)
                    .graphicsLayer {
                        alpha = contentAlpha
                        translationY = contentOffset.toPx()
                    }
                    .scrollMotionBlur(lazyListState)
            ) {
                // 1. Expressive Hero Profile Preview Card
                item(key = "profile_preview_card") {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = colors.primaryContainer.copy(alpha = 0.42f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            colors.primaryContainer.copy(alpha = 0.65f),
                                            colors.surfaceContainer.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Animated Avatar Preview with smooth spring crossfade
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(124.dp)
                                        .border(BorderStroke(3.dp, ringGradient), CircleShape)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .clickable { launchPhotoPicker() }
                                ) {
                                    AnimatedContent(
                                        targetState = uiState.effectiveLocalAvatar to uiState.profile.updatedAt,
                                        transitionSpec = {
                                            (fadeIn(tween(240)) + scaleIn(
                                                initialScale = 0.86f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )) togetherWith (fadeOut(tween(160)) + scaleOut(targetScale = 0.9f))
                                        },
                                        label = "avatarPreviewMorph"
                                    ) { (spec, key) ->
                                        ProfileAvatar(
                                            avatarSpec = spec,
                                            displayName = previewName,
                                            size = 112.dp,
                                            cacheKey = key,
                                            fallbackInitial = "G"
                                        )
                                    }
                                    if (uiState.isSavingPhoto) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 3.dp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = previewName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val avatarSubtitle = when {
                                        currentBuiltIn != null -> "${currentBuiltIn.first.label} · ${currentBuiltIn.second.label}"
                                        ProfileAvatarCatalog.isLocalFilePath(uiState.profile.avatarSpec) -> "Custom photo"
                                        isUsingGooglePhoto -> "Google account photo"
                                        isUsingInitial -> "Initial monogram"
                                        else -> "Account photo"
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = avatarSubtitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                                    ) {
                                        ProfileQuickActionChip(
                                            label = "Photo",
                                            iconRes = R.drawable.rounded_photo_library_24,
                                            selected = ProfileAvatarCatalog.isLocalFilePath(uiState.profile.avatarSpec),
                                            onClick = { launchPhotoPicker() },
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProfileQuickActionChip(
                                            label = "Surprise",
                                            iconRes = R.drawable.rounded_shuffle_24,
                                            selected = false,
                                            onClick = { viewModel.selectRandomBuiltInAvatar() },
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProfileQuickActionChip(
                                            label = "Initial",
                                            iconRes = null,
                                            selected = isUsingInitial,
                                            onClick = { viewModel.useFirstLetterAvatar() },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (hasGooglePhoto) {
                                        val gContainer by animateColorAsState(
                                            targetValue = if (isUsingGooglePhoto) colors.primaryContainer else colors.surfaceContainerHigh,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "gPhotoBg"
                                        )
                                        val gContent by animateColorAsState(
                                            targetValue = if (isUsingGooglePhoto) colors.onPrimaryContainer else colors.onSurface,
                                            label = "gPhotoFg"
                                        )
                                        Surface(
                                            onClick = { viewModel.useGoogleAccountPhoto() },
                                            shape = CircleShape,
                                            color = gContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            ) {
                                                AsyncImage(
                                                    model = uiState.ytAvatarUrl,
                                                    contentDescription = "Google account photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Keep Google account photo",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = gContent,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isUsingGooglePhoto) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Active",
                                                        tint = gContent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Display Name Card
                item(key = "profile_display_name_card") {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Display Name",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it.take(32) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Your name") },
                                placeholder = { Text("How you appear in PixelMusic & PixelJam") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = colors.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        viewModel.saveDisplayName(nameDraft)
                                        focusManager.clearFocus()
                                        PixelToast.show(context, "Display name saved")
                                    }
                                ),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surfaceContainerHigh,
                                    unfocusedContainerColor = colors.surfaceContainerHigh,
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Shown on Home, Settings & PixelJam",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.saveDisplayName(nameDraft)
                                        focusManager.clearFocus()
                                        PixelToast.show(context, "Display name saved")
                                    },
                                    enabled = nameHasUnsavedChanges,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = if (nameHasUnsavedChanges) "Save" else "Saved",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Background Color Palette Card
                item(key = "profile_bg_colors_card") {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Avatar Background",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = "${ProfileAvatarCatalog.avatars.size * ProfileAvatarCatalog.backgrounds.size} expressive combinations",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = colors.primaryContainer
                                ) {
                                    Text(
                                        text = selectedBackground.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(ProfileAvatarCatalog.backgrounds, key = { it.id }) { bg ->
                                    BackgroundColorSwatch(
                                        background = bg,
                                        selected = bg.id == selectedBgId,
                                        onClick = {
                                            selectedBgId = bg.id
                                            val activeAvatarId = currentBuiltIn?.first?.id
                                            if (activeAvatarId != null) {
                                                viewModel.selectBuiltInAvatar(activeAvatarId, bg.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Illustrated Avatars Picker Card
                item(key = "profile_avatars_grid_card") {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Illustrated Avatars",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )

                            // Expressive Pill Category Bar
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AvatarCategory.entries.forEach { category ->
                                        val isSelected = category == selectedCategory
                                        val bgColor by animateColorAsState(
                                            targetValue = if (isSelected) colors.primary else Color.Transparent,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "catBg"
                                        )
                                        val fgColor by animateColorAsState(
                                            targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                            label = "catFg"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(CircleShape)
                                                .background(bgColor)
                                                .clickable { selectedCategory = category },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = category.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = fgColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Smooth AnimatedContent 4-Column Illustrated Avatar Grid
                            AnimatedContent(
                                targetState = selectedCategory,
                                transitionSpec = {
                                    val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                    (fadeIn(tween(220)) + slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { dir * it / 8 }) togetherWith
                                        (fadeOut(tween(150)) + slideOutHorizontally { -dir * it / 8 })
                                },
                                label = "avatarCategoryGridTransition"
                            ) { cat ->
                                val list = remember(cat) {
                                    if (cat == AvatarCategory.ALL) ProfileAvatarCatalog.avatars
                                    else ProfileAvatarCatalog.avatars.filter { it.category == cat }
                                }
                                val rows = remember(list) { list.chunked(4) }
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    rows.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowItems.forEach { avatar ->
                                                val isSelected = currentBuiltIn?.first?.id == avatar.id &&
                                                    currentBuiltIn.second.id == selectedBackground.id
                                                IllustratedAvatarTile(
                                                    avatar = avatar,
                                                    background = selectedBackground,
                                                    selected = isSelected,
                                                    onClick = {
                                                        viewModel.selectBuiltInAvatar(avatar.id, selectedBackground.id)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            repeat(4 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CollapsibleCommonTopBar(
                title = "Profile",
                collapseFraction = collapseFraction,
                headerHeight = currentTopBarHeightDp,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun ProfileQuickActionChip(
    label: String,
    iconRes: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "chipScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSurface,
        label = "chipFg"
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
            .height(44.dp)
            .scale(scale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BackgroundColorSwatch(
    background: AvatarBackground,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swatchScale"
    )
    val brush = remember(background.id) {
        Brush.linearGradient(
            colors = listOf(Color(background.topColorHex), Color(background.bottomColorHex))
        )
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(brush)
            .then(
                if (selected) {
                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = background.label,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun IllustratedAvatarTile(
    avatar: BuiltInAvatar,
    background: AvatarBackground,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "avatarTileScale"
    )
    val bgBrush = remember(background.id) {
        Brush.linearGradient(
            colors = listOf(Color(background.topColorHex), Color(background.bottomColorHex))
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .then(
                        if (selected) {
                            Modifier
                                .border(2.5.dp, colors.primary, CircleShape)
                                .padding(3.dp)
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(bgBrush)
            ) {
                Image(
                    painter = painterResource(avatar.drawableRes),
                    contentDescription = avatar.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Text(
            text = avatar.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
