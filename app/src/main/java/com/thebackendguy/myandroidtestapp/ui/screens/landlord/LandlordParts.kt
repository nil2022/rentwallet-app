package com.thebackendguy.myandroidtestapp.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thebackendguy.myandroidtestapp.data.LandlordStore
import com.thebackendguy.myandroidtestapp.data.Loadable
import com.thebackendguy.myandroidtestapp.data.Portfolio
import com.thebackendguy.myandroidtestapp.data.remote.ApiResult
import com.thebackendguy.myandroidtestapp.ui.components.EmptyState
import com.thebackendguy.myandroidtestapp.ui.components.ErrorState
import com.thebackendguy.myandroidtestapp.ui.components.LoadingCards
import com.thebackendguy.myandroidtestapp.ui.components.Toasts
import com.thebackendguy.myandroidtestapp.ui.components.softShadow
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.icons.LucideIcon
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/** The landlord's data; starts loading it if nothing has loaded yet (for example after the app was restored). */
@Composable
internal fun portfolioState(): Loadable<Portfolio> {
    val state by LandlordStore.state.collectAsState()
    LaunchedEffect(Unit) { if (LandlordStore.state.value.data == null) LandlordStore.refresh() }
    return state
}

/**
 * Shows [content] once the landlord's data has loaded; until then, placeholder
 * cards, or the reason it failed with a retry button.
 */
@Composable
internal fun ColumnScope.PortfolioContent(
    state: Loadable<Portfolio>,
    loadingImageHeight: Dp = 0.dp,
    content: @Composable ColumnScope.(Portfolio) -> Unit
) {
    val data = state.data
    when {
        data != null -> content(data)
        state.error != null -> ErrorState(state.error, onRetry = LandlordStore::refresh)
        else -> LoadingCards(3, loadingImageHeight)
    }
}

/** For a details screen whose record was deleted or never loaded. */
@Composable
internal fun NotFound(what: String, onBack: () -> Unit) {
    EmptyState(
        icon = Lucide.TriangleAlert,
        title = "$what not found",
        message = "It may have been deleted. Go back to see the latest list.",
        actionLabel = "Go back",
        actionIcon = Lucide.ArrowLeft,
        onAction = onBack
    )
}

@Composable
internal fun SearchBox(query: String, placeholder: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().height(48.dp).softShadow(shape).clip(shape).background(Rf.Lowest).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LIcon(Lucide.Search, tint = Rf.Outline, strokeWidth = 1.75f)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(placeholder, style = RfType.BodyMd, color = Rf.Outline.copy(alpha = 0.8f))
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = RfType.BodyMd.copy(color = Rf.OnSurface, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Rf.Primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Grey chip with an icon, such as "4 floors" or "Lift". */
@Composable
internal fun FactChip(icon: LucideIcon, text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Rf.Low).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LIcon(icon, size = 12.dp, tint = Rf.OnSurfaceVariant)
        Text(text, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
    }
}

/** Three numbers side by side, such as rooms, let and vacant. */
@Composable
internal fun CountTiles(items: List<Triple<String, String, androidx.compose.ui.graphics.Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (count, label, color) ->
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.weight(1f).softShadow(shape).clip(shape).background(Rf.Lowest).padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(count, style = RfType.HeadlineSm.copy(fontWeight = FontWeight.Bold), color = color)
                Text(label, style = RfType.LabelSm, color = Rf.OnSurfaceVariant)
            }
        }
    }
}

/** Shows the outcome of a save or delete as a floating note; returns true on success. */
internal fun ApiResult<*>.announce(success: String): Boolean = when (this) {
    is ApiResult.Ok -> {
        Toasts.show(success)
        true
    }
    is ApiResult.Fail -> {
        Toasts.error(message)
        false
    }
}
