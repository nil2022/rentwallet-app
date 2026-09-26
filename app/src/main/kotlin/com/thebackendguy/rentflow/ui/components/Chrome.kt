package com.thebackendguy.rentflow.ui.components

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.LocalRfPalette
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import com.thebackendguy.rentflow.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Notice(val title: String, val message: String)

/* ------------------------------ Top bar ------------------------------ */

@Composable
private fun SquareButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Int = 40,
    radius: Int = 16,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(size.dp)
            .pressable(onClick)
            .clip(RoundedCornerShape(radius.dp))
            .background(Rf.Low),
        contentAlignment = Alignment.Center
    ) { content() }
}

private val DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val TIME = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US)

/** Date and ticking time chip, like the web top bar's clock. */
@Composable
private fun HeaderClock() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
        }
    }
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Rf.Low).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = now.format(DAY),
            style = RfType.LabelSm.copy(fontSize = 12.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em, fontFeatureSettings = "tnum"),
            color = Rf.OnSurface
        )
        Text(
            text = now.format(TIME).uppercase(Locale.US),
            style = RfType.LabelSm.copy(lineHeight = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.01).em, fontFeatureSettings = "tnum"),
            color = Rf.OnSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Moon in light mode, sun in dark mode (amber, like the web); switches the theme. */
@Composable
private fun ThemeButton() {
    val dark = LocalRfPalette.current.isDark
    SquareButton({ ThemeMode.set(!dark) }) {
        LIcon(
            if (dark) Lucide.Sun else Lucide.MoonStar,
            tint = Rf.Amber,
            contentDescription = if (dark) "Switch to light mode" else "Switch to dark mode"
        )
    }
}

/**
 * The web's phone top bar: menu, clock, notifications, theme and account on the first
 * row; a greeting (or back button and page title) and an optional chip below.
 */
@Composable
fun RfTopBar(
    title: String,
    name: String,
    photo: String?,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    chip: String? = null,
    notices: List<Notice> = emptyList(),
    onProfile: (() -> Unit)? = null
) {
    var showNotices by remember { mutableStateOf(false) }
    Column(
        modifier
            .zIndex(1f)
            .shadow(3.dp, RectangleShape, ambientColor = Rf.Shadow, spotColor = Rf.Shadow)
            .background(Rf.Lowest.copy(alpha = 0.97f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareButton(onMenu) { LIcon(Lucide.Menu, tint = Rf.OnSurface, contentDescription = "Open menu") }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderClock()
                Box {
                    SquareButton({ showNotices = true }) { LIcon(Lucide.Bell, tint = Rf.OnSurface, contentDescription = "Notifications") }
                    if (notices.isNotEmpty()) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .heightIn(min = 16.dp)
                                .widthIn(min = 16.dp)
                                .border(2.dp, Rf.Lowest, CircleShape)
                                .clip(CircleShape)
                                .background(Rf.Error)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = notices.size.toString(), style = RfType.LabelSm.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp), color = Rf.OnError)
                        }
                    }
                    DropdownMenu(expanded = showNotices, onDismissRequest = { showNotices = false }) {
                        if (notices.isEmpty()) {
                            DropdownMenuItem(text = { Text("You’re all caught up", style = RfType.BodyMd) }, onClick = { showNotices = false })
                        }
                        notices.forEach { notice ->
                            DropdownMenuItem(
                                text = {
                                    Column(Modifier.widthIn(max = 260.dp).padding(vertical = 4.dp)) {
                                        Text(notice.title, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
                                        Text(notice.message, style = RfType.BodySm, color = Rf.OnSurfaceVariant)
                                    }
                                },
                                onClick = { showNotices = false }
                            )
                        }
                    }
                }
                ThemeButton()
                SquareButton(onProfile, Modifier) {
                    PersonAvatar(name, photo, size = 36.dp, online = true)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                SquareButton(onBack, size = 32, radius = 12) {
                    LIcon(Lucide.ArrowLeft, size = 18.dp, tint = Rf.Primary, contentDescription = "Go back")
                }
            }
            Text(
                text = title,
                style = RfType.HeadlineSm.copy(lineHeight = 22.sp),
                color = Rf.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (chip != null) {
                Pill(chip, background = Rf.Low, content = Rf.Primary, style = RfType.LabelMd, horizontal = 12.dp, vertical = 6.dp, trailingIcon = Lucide.ChevronDown)
            }
        }
    }
}

/* ------------------------------ Bottom navigation ------------------------------ */

data class NavTab(val label: String, val icon: LucideIcon)

/** Bottom tab bar with the Material 3 pill indicator (the web's MobileBottomNav). */
@Composable
fun RfBottomNav(tabs: List<NavTab>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .zIndex(1f)
            .shadow(6.dp, RectangleShape, ambientColor = Rf.Shadow, spotColor = Rf.Shadow)
            .background(Rf.Lowest.copy(alpha = 0.97f))
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val active = index == selected
            val color by animateColorAsState(if (active) Rf.Primary else Rf.OnSurfaceVariant, label = "tabColor")
            val indicator by animateColorAsState(if (active) Rf.PrimaryFixed else Color.Transparent, tween(350), label = "tabIndicator")
            val scale by animateFloatAsState(if (active) 1f else 0.85f, tween(400, easing = IosEase), label = "tabScale")
            Column(
                Modifier
                    .widthIn(min = 56.dp)
                    .pressable(if (active) null else ({ onSelect(index) }), pressScale = 0.9f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    Modifier.size(width = 48.dp, height = 26.dp).scale(scale).clip(CircleShape).background(indicator),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(tab.icon, size = 22.dp, tint = color, strokeWidth = if (active) 2.25f else 1.75f)
                }
                Text(
                    text = tab.label,
                    style = RfType.LabelSm.copy(letterSpacing = (-0.01).em, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                    color = color
                )
            }
        }
    }
}

/* ------------------------------ Page frames ------------------------------ */

/** In-app page: top bar, scrolling content with 16dp padding and 24dp gaps, optional bottom bar. */
@Composable
fun AppPage(
    topBar: @Composable () -> Unit,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Rf.Surface)) {
        topBar()
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            content()
            if (bottomBar == null) Spacer(Modifier.navigationBarsPadding())
        }
        bottomBar?.invoke()
    }
}

/**
 * Frame for login, forgot password and registration: back button and brand
 * lockup on top, footer links at the bottom (the web's MobileAuthShell).
 * [error] floats up from the bottom until [onDismissError] clears it.
 */
@Composable
fun AuthShell(
    onBack: () -> Unit,
    error: String? = null,
    onDismissError: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Rf.Surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val minHeight = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        Modifier.size(44.dp).pressable(onBack).softShadow(shape).clip(shape).background(Rf.Low),
                        contentAlignment = Alignment.Center
                    ) {
                        LIcon(Lucide.ArrowLeft, size = 24.dp, tint = Rf.OnSurface, strokeWidth = 1.75f, contentDescription = "Go back")
                    }
                    BrandLockup(color = Rf.OnSurface, logoSize = 30.dp, fontSize = 22.sp, gap = 8.dp, modifier = Modifier.padding(start = 4.dp))
                }
                content()
                Spacer(Modifier.weight(1f))
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Terms & Conditions", "Privacy Policy", "About Us").forEachIndexed { i, link ->
                            if (i > 0) Text("•", style = RfType.LabelSm, color = Rf.OutlineVariant)
                            Text(link, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
                        }
                    }
                    Text("© 2026 $BRAND_NAME", style = RfType.LabelSm.copy(letterSpacing = 0.sp), color = Rf.Outline)
                }
            }
        }
        FloatingError(error, onDismissError, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Light or dark status- and navigation-bar icons for the current screen, and
 * the window colour that shows between screens.
 */
@Composable
fun SystemBars(darkIcons: Boolean, background: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(darkIcons, background) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
        window.setBackgroundDrawable(ColorDrawable(background.toArgb()))
        onDispose { }
    }
}
