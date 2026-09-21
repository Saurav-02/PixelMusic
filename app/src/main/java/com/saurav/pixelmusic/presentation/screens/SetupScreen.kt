package com.saurav.pixelmusic.presentation.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.backup.model.BackupSection
import com.saurav.pixelmusic.data.backup.model.BackupTransferProgressUpdate
import com.saurav.pixelmusic.data.backup.model.RestorePlan
import com.saurav.pixelmusic.data.preferences.AppThemeMode
import com.saurav.pixelmusic.presentation.components.PermissionIconCollage
import com.saurav.pixelmusic.presentation.components.BackupModuleSelectionDialog
import com.saurav.pixelmusic.presentation.components.subcomps.MaterialYouVectorDrawable
import com.saurav.pixelmusic.presentation.components.subcomps.SineWaveLine
import com.saurav.pixelmusic.presentation.components.FileExplorerDialog
import com.saurav.pixelmusic.presentation.viewmodel.DirectoryEntry
import com.saurav.pixelmusic.presentation.viewmodel.SetupEvent
import com.saurav.pixelmusic.presentation.viewmodel.SetupUiState
import com.saurav.pixelmusic.presentation.viewmodel.SetupViewModel
import com.saurav.pixelmusic.ui.theme.ExpTitleTypography
import com.saurav.pixelmusic.ui.theme.GoogleSansRounded
import com.saurav.pixelmusic.utils.StorageInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.material.icons.outlined.Person
import androidx.navigation.NavController
import com.saurav.pixelmusic.presentation.navigation.navigateSafely
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.rounded.CheckCircle
import com.saurav.pixelmusic.presentation.screens.AdvancedTokenLoginDialog
import com.saurav.pixelmusic.presentation.screens.youtube.AuthViewModel
import androidx.compose.material.icons.rounded.VpnKey
import com.saurav.pixelmusic.ui.effects.successSweepEffect

@OptIn(ExperimentalPermissionsApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupScreen(
    navController: NavController,
    setupViewModel: SetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
    val currentPath by setupViewModel.currentPath.collectAsStateWithLifecycle()
    val directoryChildren by setupViewModel.currentDirectoryChildren.collectAsStateWithLifecycle()
    val availableStorages by setupViewModel.availableStorages.collectAsStateWithLifecycle()
    val selectedStorageIndex by setupViewModel.selectedStorageIndex.collectAsStateWithLifecycle()
    val isExplorerPriming by setupViewModel.isExplorerPriming.collectAsStateWithLifecycle()
    val isExplorerReady by setupViewModel.isExplorerReady.collectAsStateWithLifecycle()
    val isCurrentDirectoryResolved by setupViewModel.isCurrentDirectoryResolved.collectAsStateWithLifecycle()
    var selectedBackupUri by remember { mutableStateOf<Uri?>(null) }
    var showAdvancedYtLoginDialog by remember { mutableStateOf(false) }
    var showCornerRadiusOverlay by remember { mutableStateOf(false) }

    // AGSL Sweep state
    var triggerSweepAnimation by remember { mutableStateOf(false) }
    var isWaitingForAuthReturn by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(triggerSweepAnimation) {
        if (triggerSweepAnimation) {
            delay(2600)
            triggerSweepAnimation = false
        }
    }

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedBackupUri = uri
        if (uri != null) {
            setupViewModel.inspectBackupFile(uri)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                setupViewModel.checkPermissions(context)
                if (isWaitingForAuthReturn && uiState.ytUsername.isNotEmpty()) {
                    triggerSweepAnimation = true
                    isWaitingForAuthReturn = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pages = remember {
        buildSetupPages(Build.VERSION.SDK_INT)
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val currentPage = pages[pagerState.currentPage]
    val isNextButtonEnabled = isPermissionGateSatisfied(context, currentPage, uiState)
    var previousPageIndex by remember { mutableStateOf(0) }
    val navigateToPage: (Int) -> Unit = { targetPage ->
        scope.launch {
            val boundedPage = targetPage.coerceIn(0, pages.lastIndex)
            if (boundedPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(boundedPage)
            }
        }
    }

    val directorySelectionPageIndex = remember(pages) { pages.indexOf(SetupPage.DirectorySelection) }
    val loginPageIndex = remember(pages) { pages.indexOf(SetupPage.Login) }

    // Trigger sweep when landing on connected state
    LaunchedEffect(pagerState.currentPage, uiState.ytUsername) {
        if (pagerState.currentPage == loginPageIndex && uiState.ytUsername.isNotEmpty()) {
            triggerSweepAnimation = true
        }
    }

    LaunchedEffect(Unit) {
        setupViewModel.events.collectLatest { event ->
            when (event) {
                is SetupEvent.Message -> {
                    Toast.makeText(context, event.value, Toast.LENGTH_LONG).show()
                }
                is SetupEvent.RestoreCompleted -> {
                    // Trigger AGSL animation on successful backup import
                    triggerSweepAnimation = true
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                    
                    if (pagerState.currentPage < pages.lastIndex) {
                        navigateToPage(pagerState.currentPage + 1)
                    } else {
                        onSetupComplete()
                    }
                }
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage > previousPageIndex) {
            val blockedPageIndex = firstBlockedForwardPageIndex(
                pages = pages,
                fromPageIndex = previousPageIndex,
                toPageIndex = pagerState.currentPage,
                context = context,
                uiState = uiState
            )
            if (blockedPageIndex != null) {
                setupViewModel.checkPermissions(context)
                pagerState.scrollToPage(blockedPageIndex)
                previousPageIndex = blockedPageIndex
                Toast.makeText(context, context.getString(R.string.toast_grant_permission_first), Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }
        }
        previousPageIndex = pagerState.currentPage

        if (pagerState.currentPage == directorySelectionPageIndex) {
            setupViewModel.loadMusicDirectories()
        }
    }

    BackHandler {
        if (pagerState.currentPage > 0) {
            navigateToPage(pagerState.currentPage - 1)
        }
    }

    val sweepModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier.successSweepEffect(isTriggered = triggerSweepAnimation)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(sweepModifier)
    ) {
        Scaffold(
            bottomBar = {
                SetupBottomBar(
                    pagerState = pagerState,
                    animated = (pagerState.currentPage != 0),
                    isNextButtonEnabled = isNextButtonEnabled,
                    isFinishButtonEnabled = uiState.allPermissionsGranted,
                    onNextClicked = {
                        val page = pages[pagerState.currentPage]
                        if (isPermissionGateSatisfied(context, page, uiState)) {
                            navigateToPage(pagerState.currentPage + 1)
                        } else {
                            setupViewModel.checkPermissions(context)
                            Toast.makeText(context, context.getString(R.string.toast_grant_permission_first), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFinishClicked = {
                        if (allRequiredPermissionsGrantedNow(context)) {
                            setupViewModel.setSetupComplete()
                            onSetupComplete()
                        } else {
                            setupViewModel.checkPermissions(context)
                            Toast.makeText(context, context.getString(R.string.toast_grant_all_permissions), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { pageIndex ->
                val page = pages[pageIndex]
                val pageOffset = pagerState.currentPageOffsetFraction

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - pageOffset.coerceIn(0f, 1f)
                            translationX = size.width * pageOffset
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        SetupPage.Welcome -> WelcomePage()
                        SetupPage.MediaPermission -> MediaPermissionPage(
                            uiState = uiState,
                            onPermissionStateUpdated = { setupViewModel.checkPermissions(context) }
                        )
                        SetupPage.BackupRestore -> BackupRestorePage(
                            uiState = uiState,
                            onImportClicked = { backupPickerLauncher.launch("*/*") },
                            onSkip = {
                                setupViewModel.clearRestorePlan()
                                selectedBackupUri = null
                                navigateToPage(pagerState.currentPage + 1)
                            }
                        )
                        SetupPage.Login -> LoginPage(
                            uiState = uiState,
                            onLoginClick = {
                                isWaitingForAuthReturn = true
                                navController.navigateSafely(com.saurav.pixelmusic.presentation.navigation.Screen.YoutubeAuth.route)
                            },
                            onAdvancedLoginClick = { showAdvancedYtLoginDialog = true }
                        )
                        SetupPage.DirectorySelection -> DirectorySelectionPage(
                            uiState = uiState,
                            currentPath = currentPath,
                            directoryChildren = directoryChildren,
                            availableStorages = availableStorages,
                            selectedStorageIndex = selectedStorageIndex,
                            isExplorerPriming = isExplorerPriming,
                            isExplorerReady = isExplorerReady,
                            isCurrentDirectoryResolved = isCurrentDirectoryResolved,
                            isAtRoot = setupViewModel.isAtRoot(),
                            explorerRoot = setupViewModel.explorerRoot(),
                            onOpenExplorer = setupViewModel::openExplorer,
                            onNavigateTo = setupViewModel::loadDirectory,
                            onNavigateUp = setupViewModel::navigateUp,
                            onRefresh = setupViewModel::refreshCurrentDirectory,
                            onPrimeExplorer = setupViewModel::primeExplorer,
                            onSkip = {
                                navigateToPage(pagerState.currentPage + 1)
                            },
                            onToggleAllowed = setupViewModel::toggleDirectoryAllowed,
                            onSelectionFinished = setupViewModel::applyPendingDirectoryRuleChanges,
                            onStorageSelected = setupViewModel::selectStorage
                        )
                        SetupPage.NotificationsPermission -> NotificationsPermissionPage(
                            uiState = uiState,
                            onPermissionStateUpdated = { setupViewModel.checkPermissions(context) }
                        )
                        SetupPage.OverlayPermission -> OverlayPermissionPage(
                            uiState = uiState,
                            onPermissionGranted = {
                                // Trigger AGSL sweep when overlay permission is granted
                                triggerSweepAnimation = true
                            },
                            onSkip = {
                                navigateToPage(pagerState.currentPage + 1)
                            }
                        )
                        SetupPage.AlarmsPermission -> AlarmsPermissionPage(
                            uiState = uiState,
                            onSkip = {
                                navigateToPage(pagerState.currentPage + 1)
                            }
                        )
                        SetupPage.BatteryOptimization -> BatteryOptimizationPage(
                            onSkip = {
                                navigateToPage(pagerState.currentPage + 1)
                            }
                        )
                        SetupPage.ThemeSelection -> ThemeSelectionPage(
                            uiState = uiState,
                            onModeSelected = setupViewModel::setAppThemeMode
                        )
                        SetupPage.PaletteSelection -> PaletteSelectionPage(
                            uiState = uiState,
                            onPaletteSelected = setupViewModel::setColorPalette
                        )
                        SetupPage.Finish -> FinishPage()
                        SetupPage.LibraryLayout -> LibraryLayoutPage(
                            uiState = uiState,
                            onModeSelected = setupViewModel::setLibraryNavigationMode,
                            onSkip = {
                                navigateToPage(pagerState.currentPage + 1)
                            }
                        )
                    }
                }
            }
        }

        if (showAdvancedYtLoginDialog) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val fullToken by authViewModel.getFullTokenString().collectAsStateWithLifecycle(initialValue = "")

            AdvancedTokenLoginDialog(
                currentCookie = fullToken,
                onDismiss = { showAdvancedYtLoginDialog = false },
                onSaveToken = { token ->
                    showAdvancedYtLoginDialog = false
                    authViewModel.saveManualCookie(token)
                    triggerSweepAnimation = true
                    Toast.makeText(context, "Cookie saved! Linking account...", Toast.LENGTH_SHORT).show()
                }
            )
        }

        val restorePlan = uiState.restorePlan
        if (restorePlan != null && selectedBackupUri != null) {
            BackupModuleSelectionDialog(
                plan = restorePlan,
                inProgress = uiState.isRestoringBackup,
                onDismiss = {
                    setupViewModel.clearRestorePlan()
                    selectedBackupUri = null
                },
                onBack = {
                    setupViewModel.clearRestorePlan()
                    selectedBackupUri = null
                },
                onSelectionChanged = setupViewModel::updateRestorePlanSelection,
                onConfirm = {
                    val uri = selectedBackupUri ?: return@BackupModuleSelectionDialog
                    selectedBackupUri = null
                    setupViewModel.restoreFromPlan(uri)
                }
            )
        }

        AnimatedVisibility(
            visible = showCornerRadiusOverlay,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            BackHandler {
                showCornerRadiusOverlay = false
            }
            NavBarCornerRadiusContent(
                initialRadius = uiState.navBarCornerRadius.toFloat(),
                onRadiusChange = { setupViewModel.setNavBarCornerRadius(it) },
                onDone = { showCornerRadiusOverlay = false },
                onBack = { showCornerRadiusOverlay = false },
                isFullWidth = uiState.navBarStyle == "full_width"
            )
        }
    }
}

@Composable
fun OverlayPermissionPage(
    uiState: SetupUiState,
    onPermissionGranted: () -> Unit = {},
    onSkip: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val grantedNow = Settings.canDrawOverlays(context)
                if (!isGranted && grantedNow) {
                    onPermissionGranted()
                }
                isGranted = grantedNow
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val overlayIcons = persistentListOf(
        R.drawable.rounded_music_note_24,
        R.drawable.rounded_search_24,
        R.drawable.rounded_album_24,
        R.drawable.rounded_play_arrow_24,
        R.drawable.rounded_check_circle_24
    )

    PermissionPageLayout(
        title = "Display Over Other Apps",
        granted = isGranted,
        description = "This allows PixelMusic to display the music recognition scanner directly over your other apps and home screen.",
        buttonText = if (isGranted) stringResource(R.string.setup_permission_granted) else "Grant Permission",
        buttonEnabled = !isGranted,
        icons = overlayIcons,
        onGrantClicked = {
            if (!isGranted) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        }
    ) {
        if (!isGranted) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip_for_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun LoginPage(
    uiState: SetupUiState,
    onLoginClick: () -> Unit,
    onAdvancedLoginClick: () -> Unit
) {
    val isLoggedIn = uiState.ytUsername.isNotEmpty()

    val dynamicGradient = Brush.sweepGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
    )

    if (isLoggedIn) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Connected!",
                    style = MaterialTheme.typography.displayMedium.copy(fontFamily = GoogleSansRounded, fontSize = 32.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your YouTube Music account is securely linked.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Outer border container with the dynamic sweep gradient ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(116.dp)
                            .border(BorderStroke(3.5.dp, dynamicGradient), CircleShape)
                    ) {
                        // Inner container creating the gap and holding avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            if (uiState.ytAvatarUrl.isNotEmpty()) {
                                coil.compose.AsyncImage(
                                    model = uiState.ytAvatarUrl,
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Person,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(24.dp).fillMaxSize()
                                )
                            }
                        }
                    }

                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = AbsoluteSmoothCornerShape(16.dp, 60)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(uiState.ytUsername, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "YouTube Music",
                    style = MaterialTheme.typography.displayMedium.copy(fontFamily = GoogleSansRounded, fontSize = 32.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Choose how you'd like to connect your library to get personalized recommendations.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PermissionIconCollage(
                    modifier = Modifier.height(220.dp),
                    icons = persistentListOf(R.drawable.rounded_music_note_24, R.drawable.rounded_library_music_24, R.drawable.rounded_search_24, R.drawable.rounded_album_24, R.drawable.rounded_playlist_play_24)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onLoginClick,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "Manual Login", 
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onAdvancedLoginClick,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "Advanced Login (Tokens)", 
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DirectorySelectionPage(
    uiState: SetupUiState,
    currentPath: File,
    directoryChildren: List<DirectoryEntry>,
    availableStorages: List<StorageInfo>,
    selectedStorageIndex: Int,
    isExplorerPriming: Boolean,
    isExplorerReady: Boolean,
    isCurrentDirectoryResolved: Boolean,
    isAtRoot: Boolean,
    explorerRoot: File,
    onOpenExplorer: () -> Unit,
    onNavigateTo: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onPrimeExplorer: () -> Unit,
    onSkip: () -> Unit,
    onToggleAllowed: (File) -> Unit,
    onSelectionFinished: () -> Unit,
    onStorageSelected: (Int) -> Unit
) {
    var showDirectoryPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val hasMediaPermission = uiState.mediaPermissionGranted
    val canOpenDirectoryPicker = hasMediaPermission

    LaunchedEffect(canOpenDirectoryPicker) {
        if (canOpenDirectoryPicker) {
            onPrimeExplorer()
        }
    }

    PermissionPageLayout(
        title = stringResource(R.string.setup_excluded_folders_title),
        description = stringResource(R.string.setup_excluded_folders_description),
        buttonText = stringResource(R.string.setup_choose_folders_ignore),
        buttonEnabled = canOpenDirectoryPicker,
        onGrantClicked = {
            if (canOpenDirectoryPicker) {
                showDirectoryPicker = true
                onOpenExplorer()
            } else {
                Toast.makeText(context, context.getString(R.string.toast_grant_storage_first), Toast.LENGTH_SHORT).show()
            }
        },
        icons = persistentListOf(
            R.drawable.rounded_folder_24,
            R.drawable.rounded_music_note_24,
            R.drawable.rounded_create_new_folder_24,
            R.drawable.rounded_folder_open_24,
            R.drawable.rounded_audio_file_24
        )
    ) {
        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.skip_for_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    FileExplorerDialog(
        visible = showDirectoryPicker,
        currentPath = currentPath,
        directoryChildren = directoryChildren,
        availableStorages = availableStorages,
        selectedStorageIndex = selectedStorageIndex,
        isLoading = uiState.isLoadingDirectories,
        isPriming = isExplorerPriming,
        isReady = isExplorerReady,
        isCurrentDirectoryResolved = isCurrentDirectoryResolved,
        isAtRoot = isAtRoot,
        rootDirectory = explorerRoot,
        onNavigateTo = onNavigateTo,
        onNavigateUp = onNavigateUp,
        onNavigateHome = { onNavigateTo(explorerRoot) },
        onToggleAllowed = onToggleAllowed,
        onRefresh = onRefresh,
        onStorageSelected = onStorageSelected,
        onDone = {
            onSelectionFinished()
            showDirectoryPicker = false
        },
        onDismiss = {
            onSelectionFinished()
            showDirectoryPicker = false
        }
    )
}

sealed class SetupPage {
    object Welcome : SetupPage()
    object MediaPermission : SetupPage()
    object BackupRestore : SetupPage()
    object Login : SetupPage()
    object DirectorySelection : SetupPage()
    object ThemeSelection : SetupPage()
    object PaletteSelection : SetupPage()
    object NotificationsPermission : SetupPage()
    object OverlayPermission : SetupPage()
    object AlarmsPermission : SetupPage()
    object LibraryLayout : SetupPage()
    object BatteryOptimization : SetupPage()
    object Finish : SetupPage()
}

private fun buildSetupPages(sdkInt: Int): List<SetupPage> {
    val pages = mutableListOf<SetupPage>(
        SetupPage.Welcome,
        SetupPage.MediaPermission
    )

    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        pages += SetupPage.NotificationsPermission
    }

    if (sdkInt >= Build.VERSION_CODES.M) {
        pages += SetupPage.OverlayPermission
    }

    pages += SetupPage.BackupRestore
    pages += SetupPage.Login
    pages += SetupPage.DirectorySelection
    pages += SetupPage.ThemeSelection
    pages += SetupPage.PaletteSelection
    pages += SetupPage.LibraryLayout

    if (sdkInt >= Build.VERSION_CODES.S) {
        pages += SetupPage.AlarmsPermission
    }

    pages += SetupPage.BatteryOptimization
    pages += SetupPage.Finish
    return pages
}

private fun firstBlockedForwardPageIndex(
    pages: List<SetupPage>,
    fromPageIndex: Int,
    toPageIndex: Int,
    context: Context,
    uiState: SetupUiState
): Int? {
    if (toPageIndex <= fromPageIndex) return null
    return (fromPageIndex until toPageIndex).firstOrNull { pageIndex ->
        !isPermissionGateSatisfied(context, pages[pageIndex], uiState)
    }
}

private fun isPermissionGateSatisfied(
    context: Context,
    page: SetupPage,
    uiState: SetupUiState
): Boolean {
    return when (page) {
        SetupPage.MediaPermission -> {
            uiState.mediaPermissionGranted || hasMediaPermissionNow(context)
        }
        SetupPage.NotificationsPermission -> {
            uiState.notificationsPermissionGranted ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        }
        else -> true
    }
}

private fun allRequiredPermissionsGrantedNow(context: Context): Boolean {
    val mediaGranted = hasMediaPermissionNow(context)
    val notificationsGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    return mediaGranted && notificationsGranted
}

private fun hasMediaPermissionNow(context: Context): Boolean {
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.setup_welcome_prefix),
                style = ExpTitleTypography.displayLarge.copy(
                    fontSize = 42.sp,
                    lineHeight = 1.1.em
                ),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontSize = 46.sp,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 1.1.em
                ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 2.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_beta_symbol),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.setup_beta_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
        ){
            MaterialYouVectorDrawable(
                modifier = Modifier.fillMaxSize(),
                drawableResId = R.drawable.welcome_art
            )
            SineWaveLine(
                modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(32.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
                animate = true,
                color = MaterialTheme.colorScheme.surface,
                alpha = 0.95f,
                strokeWidth = 16.dp,
                amplitude = 4.dp,
                waves = 7.6f,
                phase = 0f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(22.dp)
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp)
            ){}
            SineWaveLine(
                modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(32.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
                animate = true,
                color = MaterialTheme.colorScheme.primary,
                alpha = 0.95f,
                strokeWidth = 4.dp,
                amplitude = 4.dp,
                waves = 7.6f,
                phase = 0f
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.setup_intro_body), style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionPage(
    uiState: SetupUiState,
    onPermissionStateUpdated: () -> Unit
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    val mediaIcons = persistentListOf(
        R.drawable.rounded_music_note_24,
        R.drawable.rounded_album_24,
        R.drawable.rounded_library_music_24,
        R.drawable.rounded_artist_24,
        R.drawable.rounded_playlist_play_24
    )

    val isGranted = uiState.mediaPermissionGranted || permissionState.allPermissionsGranted

    LaunchedEffect(permissionState.allPermissionsGranted) {
        onPermissionStateUpdated()
    }

    PermissionPageLayout(
        title = stringResource(R.string.setup_permission_media_title),
        granted = isGranted,
        description = stringResource(R.string.setup_permission_media_description),
        buttonText = if (isGranted) stringResource(R.string.setup_permission_granted) else stringResource(R.string.setup_grant_media_permission),
        buttonEnabled = !isGranted,
        icons = mediaIcons,
        onGrantClicked = {
            if (!isGranted) {
                permissionState.launchMultiplePermissionRequest()
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationsPermissionPage(
    uiState: SetupUiState,
    onPermissionStateUpdated: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val permissionState = rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.POST_NOTIFICATIONS))
    val notificationIcons = persistentListOf(
        R.drawable.rounded_circle_notifications_24,
        R.drawable.rounded_skip_next_24,
        R.drawable.rounded_play_arrow_24,
        R.drawable.rounded_pause_24,
        R.drawable.rounded_skip_previous_24
    )

    val isGranted = uiState.notificationsPermissionGranted || permissionState.allPermissionsGranted

    LaunchedEffect(permissionState.allPermissionsGranted) {
        onPermissionStateUpdated()
    }

    PermissionPageLayout(
        title = stringResource(R.string.setup_permission_notifications_title),
        granted = isGranted,
        description = stringResource(R.string.setup_permission_notifications_description),
        buttonText = if (isGranted) stringResource(R.string.setup_permission_granted) else stringResource(R.string.setup_enable_notifications),
        buttonEnabled = !isGranted,
        icons = notificationIcons,
        onGrantClicked = {
            if (!isGranted) {
                permissionState.launchMultiplePermissionRequest()
            }
        }
    )
}

@Composable
fun AlarmsPermissionPage(
    uiState: SetupUiState,
    onSkip: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val context = LocalContext.current
    val icons = persistentListOf(
        R.drawable.rounded_alarm_24,
        R.drawable.rounded_schedule_24,
        R.drawable.rounded_timer_24,
        R.drawable.rounded_hourglass_empty_24,
        R.drawable.rounded_notifications_active_24
    )

    val isGranted = uiState.alarmsPermissionGranted

    PermissionPageLayout(
        title = stringResource(R.string.setup_permission_alarms_title),
        granted = isGranted,
        description = stringResource(R.string.setup_permission_alarms_description),
        buttonText = if (isGranted) stringResource(R.string.setup_permission_granted) else stringResource(R.string.setup_grant_permission_generic),
        buttonEnabled = !isGranted,
        icons = icons,
        onGrantClicked = {
            if (!isGranted) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                val uri = "package:${context.packageName}".toUri()
                intent.data = uri
                context.startActivity(intent)
            }
        }
    ) {
        if (!isGranted) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip_for_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupRestorePage(
    uiState: SetupUiState,
    onImportClicked: () -> Unit,
    onSkip: () -> Unit
) {
    val isBusy = uiState.isInspectingBackup || uiState.isRestoringBackup
    val progress = uiState.backupTransferProgress

    PermissionPageLayout(
        title = stringResource(R.string.setup_backup_have_title),
        description = stringResource(R.string.setup_backup_have_description),
        buttonText = when {
            uiState.isInspectingBackup -> stringResource(R.string.setup_inspecting_backup)
            uiState.isRestoringBackup -> stringResource(R.string.setup_restoring_backup)
            else -> stringResource(R.string.setup_import_backup)
        },
        buttonEnabled = !isBusy,
        icons = persistentListOf(
            R.drawable.rounded_upload_file_24,
            R.drawable.rounded_playlist_play_24,
            R.drawable.rounded_settings_24,
            R.drawable.rounded_lyrics_24,
            R.drawable.rounded_monitoring_24
        ),
        onGrantClicked = onImportClicked
    ) {
        AnimatedVisibility(
            visible = uiState.isInspectingBackup || progress != null
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.isInspectingBackup && progress == null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoadingIndicator(modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.setup_checking_backup),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (progress != null) {
                        Text(
                            text = progress.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = progress.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { progress.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onSkip,
            enabled = !uiState.isRestoringBackup
        ) {
            Text(stringResource(R.string.skip_not_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class ThemeOptionItem(
    val mode: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val recommended: Boolean = false
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionPage(
    uiState: SetupUiState,
    onModeSelected: (String) -> Unit
) {
    val themeOptions = listOf(
        ThemeOptionItem(
            mode = AppThemeMode.DARK,
            title = stringResource(R.string.setup_theme_dark_title),
            description = stringResource(R.string.setup_theme_dark_description),
            icon = Icons.Rounded.DarkMode,
        ),
        ThemeOptionItem(
            mode = AppThemeMode.LIGHT,
            title = stringResource(R.string.setup_theme_light_title),
            description = stringResource(R.string.setup_theme_light_description),
            icon = Icons.Outlined.LightMode
        ),
        ThemeOptionItem(
            mode = AppThemeMode.FOLLOW_SYSTEM,
            title = stringResource(R.string.setup_theme_follow_title),
            description = stringResource(R.string.setup_theme_follow_description),
            icon = Icons.Rounded.PhoneAndroid,
            recommended = true
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_theme_title),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.setup_theme_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            themeOptions.forEach { option ->
                ThemeModeOptionCard(
                    option = option,
                    selected = uiState.appThemeMode == option.mode,
                    onClick = { onModeSelected(option.mode) }
                )
            }

            Text(
                text = stringResource(R.string.setup_theme_footer),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PaletteSelectionPage(
    uiState: SetupUiState,
    onPaletteSelected: (String) -> Unit
) {
    val paletteOptions = listOf(
        ThemeOptionItem(
            mode = "DYNAMIC",
            title = "Dynamic (System)",
            description = "Extracts accent colors directly from your wallpaper.",
            icon = Icons.Outlined.Palette,
            recommended = true
        ),
        ThemeOptionItem(
            mode = "SAGE",
            title = "Sage Green (Mint)",
            description = "A calming, natural mint green.",
            icon = Icons.Outlined.Palette,
            recommended = false
        ),
        ThemeOptionItem(
            mode = "PURPLE",
            title = "Classic Purple",
            description = "A deep, elegant violet.",
            icon = Icons.Outlined.Palette,
            recommended = false
        ),
        ThemeOptionItem(
            mode = "BLUE",
            title = "Slate Blue",
            description = "A cool, muted blue.",
            icon = Icons.Outlined.Palette,
            recommended = false
        ),
        ThemeOptionItem(
            mode = "ORANGE",
            title = "Sunset Orange",
            description = "A warm, vibrant orange.",
            icon = Icons.Outlined.Palette,
            recommended = false
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "App Color Palette",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pick the accent colors that feel right for you.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(paletteOptions) { option ->
                ThemeModeOptionCard(
                    option = option,
                    selected = uiState.colorPalette == option.mode,
                    onClick = { onPaletteSelected(option.mode) }
                )
            }

            item {
                Text(
                    text = "You can change this later in Settings > Appearance > App Color Palette.",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeOptionCard(
    option: ThemeOptionItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (option.recommended) {
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.setup_recommended),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LibraryLayoutPage(
    uiState: SetupUiState,
    onModeSelected: (String) -> Unit,
    onSkip: () -> Unit
) {
    val isCompact = uiState.libraryNavigationMode == "compact_pill"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.setup_library_layout_title),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.setup_library_layout_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            LibraryHeaderPreview(isCompact = isCompact)
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(24.dp),
                onClick = { onModeSelected(if (isCompact) "tab_row" else "compact_pill") }
            ) {
                Row(
                   modifier = Modifier
                       .padding(horizontal = 20.dp, vertical = 16.dp)
                       .fillMaxWidth(),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setup_compact_mode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCompact) stringResource(R.string.setup_compact_mode_pill_hint) else stringResource(R.string.setup_compact_mode_tab_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCompact,
                        onCheckedChange = { checked ->
                            onModeSelected(if (checked) "compact_pill" else "tab_row")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.setup_library_layout_footer),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LibraryHeaderPreview(isCompact: Boolean) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        Color.Transparent
    )
    
    Card(
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Brush.verticalGradient(gradientColors))
        ) {
            AnimatedContent(
                targetState = isCompact,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f))
                        .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f))
                },
                label = "HeaderPreviewAnim"
            ) { compact ->
                if (compact) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 24.dp, start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LibraryNavigationPillSetupShow(
                                title = stringResource(R.string.setup_preview_songs_label),
                                isExpanded = false,
                                iconRes = R.drawable.rounded_music_note_24,
                                pageIndex = 0,
                                onClick = {},
                                onArrowClick = {}
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tab_library),
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 40.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.surfaceContainerLowest,
                                        shape = CircleShape
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                        text = stringResource(R.string.tab_songs),
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(3.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(100)
                                        )
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.surfaceContainerLowest,
                                        shape = CircleShape
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                        text = stringResource(R.string.tab_albums),
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(3.dp)
                                        .background(
                                            Color.Transparent,
                                            RoundedCornerShape(100)
                                        )
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.surfaceContainerLowest,
                                        shape = CircleShape
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                        text = stringResource(R.string.tab_artists),
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(3.dp)
                                        .background(
                                            Color.Transparent,
                                            RoundedCornerShape(100)
                                        )
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
fun BatteryOptimizationPage(
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager }
    
    var isIgnoringBatteryOptimizations by remember { 
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) 
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val batteryIcons = persistentListOf(
        R.drawable.rounded_music_note_24,
        R.drawable.rounded_play_arrow_24,
        R.drawable.rounded_all_inclusive_24,
        R.drawable.rounded_pause_24,
        R.drawable.rounded_check_circle_24
    )

    PermissionPageLayout(
        title = stringResource(R.string.setup_battery_optimization_title),
        granted = isIgnoringBatteryOptimizations,
        description = stringResource(R.string.setup_battery_optimization_description),
        buttonText = if (isIgnoringBatteryOptimizations) stringResource(R.string.setup_permission_granted) else stringResource(R.string.setup_disable_battery_optimization),
        buttonEnabled = !isIgnoringBatteryOptimizations,
        icons = batteryIcons,
        onGrantClicked = {
            if (!isIgnoringBatteryOptimizations) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(fallbackIntent)
                    } catch (e2: Exception) {
                        Toast.makeText(context, context.getString(R.string.toast_battery_settings_unavailable), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    ) {
        if (!isIgnoringBatteryOptimizations) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip_for_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun FinishPage() {
    val finishIcons = persistentListOf(
        R.drawable.rounded_check_circle_24,
        R.drawable.round_favorite_24,
        R.drawable.rounded_celebration_24,
        R.drawable.round_favorite_24,
        R.drawable.rounded_explosion_24
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.setup_all_set_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionIconCollage(
            modifier = Modifier.height(230.dp),
            icons = finishIcons
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.setup_all_set_body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PermissionPageLayout(
    title: String,
    granted: Boolean = false,
    description: String,
    buttonText: String,
    icons: ImmutableList<Int>,
    buttonEnabled: Boolean = true,
    onGrantClicked: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PermissionIconCollage(
                modifier = Modifier.height(220.dp),
                icons = icons
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGrantClicked,
                enabled = buttonEnabled,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                AnimatedContent(targetState = granted, label = "ButtonAnim") { isGranted ->
                    if (isGranted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(buttonText)
                        }
                    } else {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LibraryNavigationPillSetupShow(
    title: String,
    isExpanded: Boolean,
    iconRes: Int,
    pageIndex: Int,
    onClick: () -> Unit,
    onArrowClick: () -> Unit
) {
    data class PillState(val pageIndex: Int, val iconRes: Int, val title: String)

    val pillRadius = 26.dp
    val innerRadius = 4.dp
    val animatedArrowCorner by animateFloatAsState(
        targetValue = if (isExpanded) pillRadius.value else innerRadius.value,
        label = "ArrowCornerAnimation"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ArrowRotation"
    )

    Row(
        modifier = Modifier
            .padding(start = 4.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = pillRadius,
                bottomStart = pillRadius,
                topEnd = innerRadius,
                bottomEnd = innerRadius
            ),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(
                        topStart = pillRadius,
                        bottomStart = pillRadius,
                        topEnd = innerRadius,
                        bottomEnd = innerRadius
                    )
                )
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier.padding(start = 18.dp, end = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                AnimatedContent(
                    targetState = PillState(pageIndex = pageIndex, iconRes = iconRes, title = title),
                    transitionSpec = {
                        val direction = targetState.pageIndex.compareTo(initialState.pageIndex).coerceIn(-1, 1)
                        val slideIn = slideInHorizontally { fullWidth -> if (direction >= 0) fullWidth else -fullWidth } + fadeIn()
                        val slideOut = slideOutHorizontally { fullWidth -> if (direction >= 0) -fullWidth else fullWidth } + fadeOut()
                        slideIn.togetherWith(slideOut)
                    },
                    label = "LibraryPillTitle"
                ) { targetState ->
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = targetState.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = targetState.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = animatedArrowCorner.dp,
                bottomStart = animatedArrowCorner.dp,
                topEnd = pillRadius,
                bottomEnd = pillRadius
            ),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(
                        topStart = animatedArrowCorner.dp,
                        bottomStart = animatedArrowCorner.dp,
                        topEnd = pillRadius,
                        bottomEnd = pillRadius
                    )
                )
                .clickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onArrowClick
                )
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.rotate(arrowRotation),
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_expand_menu),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SetupBottomBar(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    pagerState: PagerState,
    onNextClicked: () -> Unit,
    onFinishClicked: () -> Unit,
    isNextButtonEnabled: Boolean,
    isFinishButtonEnabled: Boolean
) {
    val morphAnimationSpec = tween<Float>(durationMillis = 600, easing = FastOutSlowInEasing)
    val rotationAnimationSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)

    val targetShapeValues = when (pagerState.currentPage % 3) {
        0 -> listOf(50f, 50f, 50f, 50f)
        1 -> listOf(26f, 26f, 26f, 26f)
        else -> listOf(18f, 50f, 18f, 50f)
    }

    val animatedTopStart by animateFloatAsState(targetShapeValues[0], morphAnimationSpec, label = "TopStart")
    val animatedTopEnd by animateFloatAsState(targetShapeValues[1], morphAnimationSpec, label = "TopEnd")
    val animatedBottomStart by animateFloatAsState(targetShapeValues[2], morphAnimationSpec, label = "BottomStart")
    val animatedBottomEnd by animateFloatAsState(targetShapeValues[3], morphAnimationSpec, label = "BottomEnd")

    val animatedRotation by animateFloatAsState(
        targetValue = pagerState.currentPage * 360f,
        animationSpec = rotationAnimationSpec,
        label = "Rotation"
    )

    val shape = RoundedCornerShape(
        topEnd = 38.dp,
        topStart = 38.dp,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )

    Surface(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, clip = true),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = AbsoluteSmoothCornerShape(
            cornerRadiusTR = 36.dp,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = 36.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = 0.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = 0.dp,
            smoothnessAsPercentTR = 60
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "StepTextAnimation"
                ) { targetPage ->
                    if (targetPage == 0) {
                        Text(
                            text = stringResource(R.string.setup_lets_go),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.setup_step_format,
                                targetPage,
                                pagerState.pageCount - 1
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
                val isPrimaryButtonEnabled = if (isLastPage) isFinishButtonEnabled else isNextButtonEnabled
                val containerColor = if (!isPrimaryButtonEnabled) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val contentColor = if (!isPrimaryButtonEnabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }

                MediumExtendedFloatingActionButton(
                    onClick = if (isLastPage) onFinishClicked else onNextClicked,
                    shape = AbsoluteSmoothCornerShape(
                        cornerRadiusTL = animatedTopStart.toInt().dp,
                        smoothnessAsPercentTR = 60,
                        cornerRadiusTR = animatedTopEnd.toInt().dp,
                        smoothnessAsPercentTL = 60,
                        cornerRadiusBL = animatedBottomStart.toInt().dp,
                        smoothnessAsPercentBL = 60,
                        cornerRadiusBR = animatedBottomEnd.toInt().dp,
                        smoothnessAsPercentBR = 60,
                    ),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    containerColor = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .rotate(animatedRotation)
                        .padding(end = 0.dp)
                ) {
                    AnimatedContent(
                        modifier = Modifier.rotate(-animatedRotation),
                        targetState = pagerState.currentPage < pagerState.pageCount - 1,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.9f, animationSpec = tween(220, delayMillis = 90)),
                                initialContentExit = fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.9f, animationSpec = tween(90))
                            ).using(SizeTransform(clip = false))
                        },
                        label = "AnimatedFabIcon"
                    ) { isNextPage ->
                        if (isNextPage) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.cd_next_step))
                        } else {
                            if (isFinishButtonEnabled) {
                                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.cd_finish))
                            } else {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavBarCornerRadiusContent(
    initialRadius: Float,
    onRadiusChange: (Int) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    isFullWidth: Boolean
) {
    // Retained as-is
}
