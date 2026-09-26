package com.thebackendguy.rentflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import kotlin.math.min

// The web's sidebar colours: navy in both themes
private val Navy = Color(0xFF1B2A4B)
private val NavText = Color.White.copy(alpha = 0.72f)
private val NavMuted = Color.White.copy(alpha = 0.5f)
private val NavCard = Color.White.copy(alpha = 0.07f)

/**
 * The side drawer, as the web's phone sidebar: 260 dp wide (at most 85% of the
 * screen) and navy in both themes, with the logo on top, the role's [tabs]
 * with [selected] highlighted, and the signed-in person's card with log out at
 * the bottom.
 */
@Composable
fun AppDrawer(
    tabs: List<NavTab>,
    selected: Int?,
    tagline: String,
    name: String,
    roleLabel: String,
    photo: String?,
    onSelect: (Int) -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val width = min(260f, LocalConfiguration.current.screenWidthDp * 0.85f).dp
    ModalDrawerSheet(
        modifier = Modifier.width(width),
        drawerShape = RectangleShape,
        drawerContainerColor = Navy,
        windowInsets = WindowInsets(0)
    ) {
        Column(Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandLogo(size = 38.dp)
                Column {
                    Text(BRAND_NAME, style = RfType.BodyLg.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp), color = Color.White)
                    Text(tagline, style = RfType.LabelSm.copy(fontWeight = FontWeight.Normal), color = NavMuted)
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                tabs.forEachIndexed { index, tab -> DrawerItem(tab, active = index == selected) { onSelect(index) } }
            }

            Row(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavCard)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.weight(1f).pressable(onProfile, pressScale = 0.98f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PersonAvatar(name, photo, size = 34.dp, background = Rf.Logo, content = Color.White)
                    Column(Modifier.weight(1f)) {
                        Text(
                            name, style = RfType.LabelMd.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(roleLabel, style = RfType.LabelSm.copy(fontWeight = FontWeight.Normal), color = NavMuted)
                    }
                }
                Box(
                    Modifier.size(32.dp).pressable(onLogout).clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(Lucide.LogOut, size = 16.dp, tint = NavMuted, contentDescription = "Log out")
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(tab: NavTab, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val content = if (active) Color.White else NavText
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .pressable(if (active) null else onClick, pressScale = 0.98f)
            .then(if (active) Modifier.softShadow(shape, elevation = 6.dp, color = Rf.Logo.copy(alpha = 0.35f)) else Modifier)
            .clip(shape)
            .background(if (active) Rf.Logo else Color.Transparent)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LIcon(tab.icon, size = 18.dp, tint = content, strokeWidth = if (active) 2.2f else 1.8f)
        Text(
            tab.label,
            style = RfType.BodyMd.copy(fontSize = 13.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
            color = content
        )
    }
}
