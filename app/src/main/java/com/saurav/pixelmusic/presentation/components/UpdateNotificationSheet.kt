package com.saurav.pixelmusic.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saurav.pixelmusic.R
import com.saurav.pixelmusic.presentation.components.subcomps.MaterialYouVectorDrawable
import com.saurav.pixelmusic.presentation.components.subcomps.SineWaveLine
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNotificationSheet(
    isUpdateAvailable: Boolean,
    versionName: String,
    changelog: String?,
    isTestBuild: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmClick: () -> Unit,
    onMigrateClick: () -> Unit = {},
    onSnoozeClick: (() -> Unit)? = null
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            // Top Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isUpdateAvailable) "Update Available! 🚀" else "Changes in latest version ✨",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = AbsoluteSmoothCornerShape(12.dp, 60),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SineWaveLine(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(horizontal = 8.dp),
                    animate = true,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                    alpha = 0.9f,
                    strokeWidth = 3.dp,
                    amplitude = 3.dp,
                    waves = 7.6f,
                    phase = 0f
                )
            }

            // Scrollable Rich Changelog Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RichChangelogContent(changelog = changelog)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Fixed Bottom Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 20.dp)
                ) {
                    if (isTestBuild) {
                        Text(
                            text = "⚠️ This is a testing version of the app. It may contain bugs and unstable features.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        Button(
                            onClick = {
                                onMigrateClick()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(bottom = 8.dp),
                            shape = AbsoluteSmoothCornerShape(16.dp, 60)
                        ) {
                            Text(
                                text = "Migrate to Stable Release",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onConfirmClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = AbsoluteSmoothCornerShape(18.dp, 60),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isUpdateAvailable) Icons.Rounded.Download else Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUpdateAvailable) "Update Now" else "Awesome",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    if (isUpdateAvailable && onSnoozeClick != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                onSnoozeClick()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Don't remind me today",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichChangelogContent(
    changelog: String?,
    modifier: Modifier = Modifier
) {
    val items = remember(changelog) { parseChangelog(changelog) }

    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                MaterialYouVectorDrawable(
                    modifier = Modifier.fillMaxSize(),
                    drawableResId = R.drawable.welcome_art
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to the latest PixelMusic! 🎉",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Includes under-the-hood fixes and performance polish.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            when (item) {
                is ChangelogItem.Notice -> NoticeCard(item)
                is ChangelogItem.VersionHeader -> VersionHeaderView(item)
                is ChangelogItem.SectionHeader -> SectionHeaderView(item)
                is ChangelogItem.Bullet -> BulletItemView(item)
                is ChangelogItem.Image -> ImageItemView(item)
                is ChangelogItem.Paragraph -> ParagraphView(item)
                is ChangelogItem.Divider -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: ChangelogItem.Notice) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    Surface(
        shape = AbsoluteSmoothCornerShape(16.dp, 60),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = notice.icon, fontSize = 15.sp)
                    }
                }
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            notice.lines.forEach { line ->
                Text(
                    text = parseRichMarkdown(line, primaryColor, tertiaryColor, codeBg, highlightBg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun VersionHeaderView(header: ChangelogItem.VersionHeader) {
    val (containerColor, contentColor, icon) = when (header.badgeColorType) {
        BadgeType.PRIMARY -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "🌟"
        )
        BadgeType.SECONDARY -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "🚀"
        )
        BadgeType.TERTIARY -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "🧪"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = AbsoluteSmoothCornerShape(12.dp, 60),
            color = containerColor,
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon, fontSize = 15.sp)
                Text(
                    text = header.cleanName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun SectionHeaderView(header: ChangelogItem.SectionHeader) {
    Text(
        text = header.title,
        style = if (header.level <= 2) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun BulletItemView(bullet: ChangelogItem.Bullet) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    Surface(
        shape = AbsoluteSmoothCornerShape(14.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!bullet.emoji.isNullOrBlank()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = bullet.emoji, fontSize = 17.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp, start = 4.dp, end = 4.dp)
                        .size(8.dp)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (!bullet.title.isNullOrBlank()) {
                    Text(
                        text = parseRichMarkdown(bullet.title, primaryColor, tertiaryColor, codeBg, highlightBg),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = parseRichMarkdown(bullet.body, primaryColor, tertiaryColor, codeBg, highlightBg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (bullet.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val uriHandler = LocalUriHandler.current
                        bullet.tags.forEach { contributor ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.clickable {
                                    uriHandler.openUri("https://github.com/$contributor")
                                }
                            ) {
                                Text(
                                    text = "@$contributor",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
private fun ImageItemView(img: ChangelogItem.Image) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column {
            AsyncImage(
                model = img.url,
                contentDescription = img.alt ?: "Changelog Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp),
                contentScale = ContentScale.Crop
            )
            if (!img.alt.isNullOrBlank()) {
                Text(
                    text = img.alt,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ParagraphView(para: ChangelogItem.Paragraph) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightBg = MaterialTheme.colorScheme.primaryContainer

    Text(
        text = parseRichMarkdown(para.text, primaryColor, tertiaryColor, codeBg, highlightBg),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

private fun parseRichMarkdown(
    text: String,
    primaryColor: Color,
    tertiaryColor: Color,
    codeBg: Color,
    highlightBg: Color
): AnnotatedString = buildAnnotatedString {
    val pattern = Regex(
        """\[([^\]]+)\]\((https?://[^\s)]+)\)|\[?@([a-zA-Z0-9_-]+)\]?|\*\*([^*]+)\*\*|`([^`]+)`|==([^=]+)==|\*([^*]+)\*"""
    )
    var currentIndex = 0
    pattern.findAll(text).forEach { match ->
        if (match.range.first > currentIndex) {
            append(text.substring(currentIndex, match.range.first))
        }
        val groups = match.groups
        when {
            // [text](url)
            groups[1] != null && groups[2] != null -> {
                val label = groups[1]!!.value
                val url = groups[2]!!.value
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    )
                ) {
                    append(label)
                }
            }
            // @user or [@user]
            groups[3] != null -> {
                val user = groups[3]!!.value
                withLink(
                    LinkAnnotation.Url(
                        url = "https://github.com/$user",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = tertiaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    )
                ) {
                    append("@$user")
                }
            }
            // **bold**
            groups[4] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(groups[4]!!.value)
                }
            }
            // `code`
            groups[5] != null -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBg,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(" ${groups[5]!!.value} ")
                }
            }
            // ==highlight==
            groups[6] != null -> {
                withStyle(
                    SpanStyle(
                        background = highlightBg,
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(" ${groups[6]!!.value} ")
                }
            }
            // *italic*
            groups[7] != null -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(groups[7]!!.value)
                }
            }
        }
        currentIndex = match.range.last + 1
    }
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}

private enum class BadgeType {
    PRIMARY,
    SECONDARY,
    TERTIARY
}

private sealed class ChangelogItem {
    data class Notice(val icon: String, val title: String, val lines: List<String>) : ChangelogItem()
    data class VersionHeader(val raw: String, val cleanName: String, val badgeColorType: BadgeType) : ChangelogItem()
    data class SectionHeader(val title: String, val level: Int) : ChangelogItem()
    data class Bullet(
        val emoji: String?,
        val title: String?,
        val body: String,
        val tags: List<String>
    ) : ChangelogItem()
    data class Image(val url: String, val alt: String?) : ChangelogItem()
    data class Paragraph(val text: String) : ChangelogItem()
    object Divider : ChangelogItem()
}

private fun parseChangelog(rawText: String?): List<ChangelogItem> {
    if (rawText.isNullOrBlank()) return emptyList()
    val items = mutableListOf<ChangelogItem>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trim()

        if (line.isEmpty()) {
            i++
            continue
        }

        // 1. Notice / Alert Block
        if (line.startsWith("⚠️") || line.startsWith("🚨") || line.startsWith("💡") ||
            line.startsWith("📌") || line.contains("IMPORTANT NOTICE", ignoreCase = true) ||
            line.contains("NOTICE:", ignoreCase = true) || line.startsWith("[notice]", ignoreCase = true)
        ) {
            val icon = when {
                line.startsWith("🚨") -> "🚨"
                line.startsWith("💡") -> "💡"
                line.startsWith("📌") -> "📌"
                else -> "⚠️"
            }
            val titleClean = line.replace(Regex("^[⚠️🚨💡📌\\s*#]+"), "")
                .replace(Regex("\\*\\*"), "")
                .trim()
            val noticeLines = mutableListOf<String>()
            i++
            while (i < lines.size) {
                val nextLine = lines[i].trim()
                if (nextLine.startsWith("---") || nextLine.startsWith("___") ||
                    isVersionHeaderLine(nextLine) || nextLine.startsWith("# ")
                ) {
                    break
                }
                if (nextLine.isNotEmpty()) {
                    noticeLines.add(nextLine)
                }
                i++
            }
            items.add(ChangelogItem.Notice(icon = icon, title = titleClean, lines = noticeLines))
            continue
        }

        // 2. Horizontal Divider
        if (line.matches(Regex("^-{3,}|_{3,}|\\*{3,}$"))) {
            items.add(ChangelogItem.Divider)
            i++
            continue
        }

        // 3. Image: ![alt](url) or <img src="url" /> or standalone image url
        val mdImageMatch = Regex("""^!\[(.*?)\]\((https?://[^\s)]+)\)""").find(line)
        val htmlImageMatch = Regex("""^<img\s+[^>]*src=["'](https?://[^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE).find(line)
        val standaloneImageMatch = Regex("""^(https?://[^\s]+\.(?:png|jpg|jpeg|webp|gif)(?:\?[^\s]*)?)$""", RegexOption.IGNORE_CASE).find(line)

        if (mdImageMatch != null) {
            val alt = mdImageMatch.groupValues[1]
            val url = mdImageMatch.groupValues[2]
            items.add(ChangelogItem.Image(url = url, alt = alt.ifBlank { null }))
            i++
            continue
        } else if (htmlImageMatch != null) {
            val url = htmlImageMatch.groupValues[1]
            items.add(ChangelogItem.Image(url = url, alt = null))
            i++
            continue
        } else if (standaloneImageMatch != null) {
            val url = standaloneImageMatch.groupValues[1]
            items.add(ChangelogItem.Image(url = url, alt = null))
            i++
            continue
        }

        // 4. Version Header
        if (isVersionHeaderLine(line)) {
            val cleanName = line
                .replace(Regex("^[-*#\\s]+|[-*#\\s]+$"), "")
                .trim()
            val badgeType = when {
                cleanName.contains("Final", ignoreCase = true) || cleanName.contains("Stable", ignoreCase = true) -> BadgeType.PRIMARY
                cleanName.contains("Sub release", ignoreCase = true) || cleanName.contains("Patch", ignoreCase = true) -> BadgeType.SECONDARY
                cleanName.contains("Beta", ignoreCase = true) || cleanName.contains("Alpha", ignoreCase = true) || cleanName.contains("test", ignoreCase = true) -> BadgeType.TERTIARY
                else -> BadgeType.PRIMARY
            }
            items.add(ChangelogItem.VersionHeader(raw = line, cleanName = cleanName, badgeColorType = badgeType))
            i++
            continue
        }

        // 5. Section Header
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val title = line.drop(level).trim().replace(Regex("\\*\\*"), "")
            items.add(ChangelogItem.SectionHeader(title = title, level = level))
            i++
            continue
        }

        // 6. Bullet Items
        val bulletMatch = Regex("""^(\*|-|•|\d+\.)\s+(.*)""").find(line)
        if (bulletMatch != null) {
            val rawContent = bulletMatch.groupValues[2].trim()

            val emojiMatch = Regex("""^([\p{So}\p{Sk}\p{Sm}\p{Sc}\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27BF\u2300-\u23FF\u2B50\uFE0F]+)\s*""").find(rawContent)
            val emoji = emojiMatch?.groupValues?.get(1)?.trim()
            val afterEmoji = if (emojiMatch != null) rawContent.substring(emojiMatch.range.last + 1).trim() else rawContent

            val titleMatch = Regex("""^\*\*([^*]+)\*\*[:\-]?\s*(.*)""").find(afterEmoji)
            val title = titleMatch?.groupValues?.get(1)?.trim()
            val body = if (titleMatch != null) titleMatch.groupValues[2].trim() else afterEmoji

            val contributorRegex = Regex("""\[@([a-zA-Z0-9_\-]+)\]""")
            val contributors = contributorRegex.findAll(body).map { it.groupValues[1] }.toList()
            val bodyClean = body.replace(contributorRegex, "").trim()

            items.add(
                ChangelogItem.Bullet(
                    emoji = emoji,
                    title = title,
                    body = bodyClean.ifBlank { title ?: "" },
                    tags = contributors
                )
            )
            i++
            continue
        }

        // 7. Fallback Paragraph
        items.add(ChangelogItem.Paragraph(text = line))
        i++
    }

    return items
}

private fun isVersionHeaderLine(line: String): Boolean {
    val trimmed = line.trim()
    val stripped = trimmed.replace(Regex("^[-*#\\s]+|[-*#\\s]+$"), "").trim()
    if (stripped.isEmpty()) return false
    val lower = stripped.lowercase()
    return lower.startsWith("sub release") ||
           lower.startsWith("final release") ||
           lower.startsWith("release") ||
           lower.startsWith("version") ||
           lower.startsWith("pixel music beta") ||
           lower.startsWith("pixel music alpha") ||
           lower.startsWith("beta-") ||
           lower.startsWith("v4.") ||
           lower.startsWith("v3.") ||
           lower.startsWith("v2.") ||
           lower.startsWith("v1.") ||
           lower.startsWith("v0.")
}
