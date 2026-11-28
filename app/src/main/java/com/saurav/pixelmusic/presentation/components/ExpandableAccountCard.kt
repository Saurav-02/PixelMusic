package com.saurav.pixelmusic.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.data.remote.youtube.YtAccount
import com.saurav.pixelmusic.presentation.viewmodel.SettingsUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableAccountCard(
    uiState: SettingsUiState,
    accounts: List<YtAccount>,
    isPro: Boolean,
    onLoginNew: () -> Unit,
    onSwitchAccount: (Int) -> Unit,
    onRemoveAccount: (Int) -> Unit,
    onManageAccounts: () -> Unit,
    onEditProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showProfileSheet by remember { mutableStateOf(false) }

    val userProfile = uiState.userProfile
    val hasYtAccount = uiState.ytUsername.isNotEmpty()
    val nameText = userProfile.effectiveDisplayName(uiState.ytUsername, "Guest User")
    val avatarSpec = userProfile.effectiveLocalAvatar(if (hasYtAccount) uiState.ytAvatarUrl else "")
    val hasProfile = hasYtAccount || userProfile.displayName.isNotBlank() || avatarSpec.isNotBlank()
    val handleText = when {
        hasYtAccount && uiState.ytHandle.isNotBlank() -> uiState.ytHandle
        hasYtAccount -> uiState.ytUsername
        userProfile.displayName.isNotBlank() || avatarSpec.isNotBlank() -> "Tap to view profile & accounts"
        else -> "Sign in to sync"
    }

    val dynamicGradient = Brush.sweepGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
    )

    Surface(
        onClick = { showProfileSheet = true },
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .then(
                        if (hasProfile) Modifier.border(BorderStroke(2.5.dp, dynamicGradient), CircleShape)
                        else Modifier
                    )
                    .clip(CircleShape)
            ) {
                ProfileAvatar(
                    avatarSpec = avatarSpec,
                    displayName = nameText,
                    size = 56.dp,
                    cacheKey = userProfile.updatedAt,
                    fallbackInitial = "G"
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = nameText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = handleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Open profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    if (showProfileSheet) {
        ProfileOverviewBottomSheet(
            displayName = nameText,
            channelHandle = if (hasYtAccount) uiState.ytHandle.ifBlank { uiState.ytUsername } else "",
            avatarSpec = avatarSpec,
            avatarCacheKey = userProfile.updatedAt,
            hasProfileRing = hasProfile,
            onDismiss = { showProfileSheet = false },
            onEditProfile = onEditProfile,
            onOpenAccounts = onManageAccounts
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileOverviewBottomSheet(
    displayName: String,
    channelHandle: String,
    avatarSpec: String,
    avatarCacheKey: Long,
    hasProfileRing: Boolean,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenAccounts: () -> Unit
) {
    val context = LocalContext.current
    val joinedLabel = remember(context) {
        val installTime = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(System.currentTimeMillis())
        val formatted = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(installTime))
        "Joined $formatted"
    }

    val greetingText = remember(displayName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val cleanName = displayName.trim().takeIf { it.isNotEmpty() && !it.equals("Guest User", ignoreCase = true) }
        when (hour) {
            in 5..11 -> if (cleanName != null) "Good morning, $cleanName! Ready to start the day with great music?" else "Good morning! Ready to start the day with great music?"
            in 12..16 -> if (cleanName != null) "Good afternoon, $cleanName! Hope your day sounds awesome." else "Good afternoon! Hope your day sounds awesome."
            in 17..21 -> if (cleanName != null) "Good evening, $cleanName! Time to unwind with your favorites." else "Good evening! Time to unwind with your favorites."
            else -> if (cleanName != null) "Late-night vibes, $cleanName? Enjoy the music." else "Late-night vibes? Enjoy the music."
        }
    }

    SmoothModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val smoothDismiss = LocalSmoothDismiss.current
        val colors = MaterialTheme.colorScheme
        var contentVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            contentVisible = true
        }

        val ringGradient = Brush.sweepGradient(
            colors = listOf(
                colors.primary,
                colors.tertiary,
                colors.secondary,
                colors.primaryContainer,
                colors.primary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top-left avatar + name + handle + joined badge
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { it / 6 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(84.dp)
                            .then(
                                if (hasProfileRing) Modifier.border(BorderStroke(3.dp, ringGradient), CircleShape)
                                else Modifier
                            )
                            .clip(CircleShape)
                    ) {
                        ProfileAvatar(
                            avatarSpec = avatarSpec,
                            displayName = displayName,
                            size = 72.dp,
                            cacheKey = avatarCacheKey,
                            fallbackInitial = "G"
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (channelHandle.isNotBlank()) {
                            Text(
                                text = channelHandle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = colors.secondaryContainer.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = joinedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Expressive Greeting Surface
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(300, delayMillis = 60, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { it / 5 }
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.primaryContainer.copy(alpha = 0.48f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                    )
                }
            }

            // Two Expressive Pill Buttons: Edit Profile & Accounts
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(320, delayMillis = 110, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { it / 4 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { smoothDismiss { onEditProfile() } },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_edit_24),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit profile",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = { smoothDismiss { onOpenAccounts() } },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_manage_accounts_24),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accounts",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
