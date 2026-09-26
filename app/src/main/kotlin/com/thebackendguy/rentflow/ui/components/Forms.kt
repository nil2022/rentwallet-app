package com.thebackendguy.rentflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.ui.icons.LIcon
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.icons.LucideIcon
import com.thebackendguy.rentflow.ui.theme.Rf
import com.thebackendguy.rentflow.ui.theme.RfType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ------------------------------ Sections ------------------------------ */

/** Small uppercase heading with an icon, above a white card holding fields 16dp apart. */
@Composable
fun FormSection(icon: LucideIcon, title: String, caption: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LIcon(icon, size = 14.dp, tint = Rf.Primary)
            Text(
                title.uppercase(Locale.ROOT),
                style = RfType.LabelSm.copy(letterSpacing = 0.08.em),
                color = Rf.OnSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (caption != null) Caption(caption)
        }
        RfCard(spacing = 16.dp, content = content)
    }
}

@Composable
fun FieldLabel(text: String, required: Boolean = false) {
    Row {
        Text(text, style = RfType.LabelMd, color = Rf.OnSurface)
        if (required) Text(" *", style = RfType.LabelMd, color = Rf.Error)
    }
}

@Composable
private fun FieldNote(error: String?, hint: String?) {
    val message = error ?: hint ?: return
    Text(
        text = message,
        style = RfType.BodySm.copy(fontSize = 12.sp, lineHeight = 16.sp),
        color = if (error != null) Rf.Error else Rf.Outline
    )
}

/* ------------------------------ Choices ------------------------------ */

/** Pick one option from a row of chips (property type, occupancy, lease length). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    required: Boolean = false,
    hint: String? = null,
    error: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, option ->
                val on = index == selected
                val bg by animateColorAsState(if (on) Rf.PrimaryFixed else Rf.Lowest, label = "chipBg")
                Row(
                    Modifier
                        .pressable({ onSelect(index) }, pressScale = 0.96f)
                        .clip(CircleShape)
                        .background(bg)
                        .border(if (on) 1.5.dp else 1.dp, if (on) Rf.Primary else Rf.Container, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (on) LIcon(Lucide.Check, size = 14.dp, tint = Rf.Primary, strokeWidth = 2.5f)
                    Text(
                        option,
                        style = RfType.LabelMd.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (on) Rf.Primary else Rf.OnSurfaceVariant
                    )
                }
            }
        }
        FieldNote(error, hint)
    }
}

/** On/off row with an icon (parking, lift, furnished). */
@Composable
fun SwitchRow(icon: LucideIcon, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, sub: String? = null) {
    Row(
        Modifier.fillMaxWidth().pressable({ onCheckedChange(!checked) }, pressScale = 1f).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LIcon(icon, size = 20.dp, tint = Rf.Primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold), color = Rf.OnSurface)
            if (sub != null) Text(sub, style = RfType.LabelSm.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurfaceVariant)
        }
        RfSwitch(checked)
    }
}

@Composable
private fun RfSwitch(checked: Boolean) {
    val track by animateColorAsState(if (checked) Rf.Primary else Rf.Container, label = "switchTrack")
    val offset by animateDpAsState(if (checked) 16.dp else 0.dp, tween(220), label = "switchThumb")
    Box(Modifier.size(width = 40.dp, height = 24.dp).clip(CircleShape).background(track).padding(3.dp)) {
        Box(Modifier.offset(x = offset).size(18.dp).shadow(1.dp, CircleShape).clip(CircleShape).background(Color.White))
    }
}

/** Number with minus and plus buttons (floors, rooms). */
@Composable
fun CountStepper(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, min: Int = 1, max: Int = 200) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label, required = true)
        val shape = RoundedCornerShape(12.dp)
        Row(
            Modifier.fillMaxWidth().height(48.dp).softShadow(shape).clip(shape).background(Rf.Lowest).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepButton(Lucide.Minus, enabled = value > min) { onChange(value - 1) }
            Text("$value", style = RfType.BodyLg.copy(fontWeight = FontWeight.Bold), color = Rf.OnSurface)
            StepButton(Lucide.Plus, enabled = value < max) { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepButton(icon: LucideIcon, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).pressable(if (enabled) onClick else null).clip(RoundedCornerShape(10.dp)).background(Rf.Low),
        contentAlignment = Alignment.Center
    ) {
        LIcon(icon, size = 16.dp, tint = if (enabled) Rf.Primary else Rf.OutlineVariant)
    }
}

/* ------------------------------ Pickers ------------------------------ */

/** Looks like a text field; tapping it opens a list of [options]. */
@Composable
fun SelectField(
    label: String,
    value: String?,
    options: List<String>,
    onSelect: (Int) -> Unit,
    icon: LucideIcon,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose",
    required: Boolean = false,
    enabled: Boolean = true,
    hint: String? = null,
    error: String? = null
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label, required)
        Box {
            FieldBox(icon = icon, error = error != null, enabled = enabled, onClick = if (enabled) ({ open = true }) else null) {
                Text(
                    text = value ?: placeholder,
                    style = RfType.BodyMd.copy(fontWeight = if (value != null) FontWeight.Medium else FontWeight.Normal),
                    color = if (value != null) Rf.OnSurface else Rf.Outline.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                LIcon(if (enabled) Lucide.ChevronDown else Lucide.Lock, size = if (enabled) 18.dp else 16.dp, tint = Rf.Outline)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option, style = RfType.BodyMd, color = Rf.OnSurface) },
                        onClick = {
                            open = false
                            onSelect(index)
                        }
                    )
                }
            }
        }
        FieldNote(error, hint)
    }
}

private val DateLabel = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/** Looks like a text field; tapping it opens a calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, date: LocalDate, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier, required: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label, required)
        FieldBox(icon = Lucide.Calendar, onClick = { open = true }) {
            Text(date.format(DateLabel), style = RfType.BodyMd.copy(fontWeight = FontWeight.Medium), color = Rf.OnSurface, modifier = Modifier.weight(1f))
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK", style = RfType.LabelMd, color = Rf.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel", style = RfType.LabelMd, color = Rf.OnSurfaceVariant) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** The white rounded box every field uses. */
@Composable
private fun FieldBox(
    icon: LucideIcon,
    error: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressable(onClick, pressScale = 0.99f)
            .then(if (enabled) Modifier.softShadow(shape) else Modifier)
            .clip(shape)
            .background(if (enabled) Rf.Lowest else Rf.Low)
            .border(if (error) 1.5.dp else 0.dp, if (error) Rf.Error else Color.Transparent, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LIcon(icon, size = 20.dp, tint = Rf.Outline, strokeWidth = 1.75f)
        content()
    }
}

/** Several lines of free text (description, amenities). */
@Composable
fun TextArea(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .softShadow(shape)
                .clip(shape)
                .background(Rf.Lowest)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) Text(placeholder, style = RfType.BodyMd, color = Rf.Outline.copy(alpha = 0.8f))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = RfType.BodyMd.copy(color = Rf.OnSurface, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Rf.Primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/* ------------------------------ Photos ------------------------------ */

const val MAX_PHOTOS = 10

/** Photo tiles four to a row; the first is the cover. Ends with an "Add" tile. */
@Composable
fun PhotoGrid(photos: List<Photo>, onChange: (List<Photo>) -> Unit, max: Int = MAX_PHOTOS) {
    val addPhotos = photoChooser(limit = max) { uris -> onChange((photos + uris.map { Photo.Picked(it) }).take(max)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Photo positions, then null for the "Add" tile
        val cells: List<Int?> = photos.indices.toList() + if (photos.size < max) listOf(null) else emptyList()
        cells.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { index ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (index == null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .pressable(addPhotos)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Rf.Lowest)
                                    .border(1.5.dp, Rf.OutlineVariant, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    LIcon(Lucide.ImagePlus, size = 20.dp, tint = Rf.Primary)
                                    Text("Add", style = RfType.LabelSm, color = Rf.Primary)
                                }
                            }
                        } else {
                            PhotoTile(photos[index], cover = index == 0) { onChange(photos.filterIndexed { i, _ -> i != index }) }
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Text(
            "${photos.size} of $max photos. The first one is the cover.",
            style = RfType.BodySm.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = Rf.Outline
        )
    }
}

@Composable
private fun PhotoTile(photo: Photo, cover: Boolean, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.fillMaxSize().clip(shape)) {
        RemoteImage(
            model = when (photo) {
                is Photo.Stored -> photo.url
                is Photo.Picked -> photo.uri
            },
            modifier = Modifier.fillMaxSize(),
            placeholderIcon = Lucide.Image
        )
        if (cover) {
            Text(
                "COVER",
                style = RfType.LabelSm.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Rf.InverseSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .pressable(onRemove)
                .clip(CircleShape)
                .background(Rf.InverseSurface.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            LIcon(Lucide.X, size = 12.dp, tint = Color.White, strokeWidth = 2.5f, contentDescription = "Remove photo")
        }
    }
}

/* ------------------------------ Bottom bar ------------------------------ */

/** Sheet-like bar pinned under a form: an optional secondary button and the main one. */
@Composable
fun FormBar(
    primary: String,
    onPrimary: () -> Unit,
    loading: Boolean = false,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Row(
        Modifier
            .zIndex(1f)
            .shadow(12.dp, shape, ambientColor = Rf.Shadow, spotColor = Rf.Shadow)
            .background(Rf.Lowest, shape)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (secondary != null && onSecondary != null) {
            TonalButton(secondary, onClick = onSecondary, modifier = Modifier.width(120.dp))
        }
        PrimaryButton(primary, onClick = onPrimary, loading = loading, enabled = enabled, showArrow = false, modifier = Modifier.weight(1f))
    }
}

/** Thin divider used between switch rows. */
@Composable
fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = Rf.Low)
}

/** Soft indigo fill behind pictures that are still loading or missing. */
val PhotoPlaceholder: Brush
    @Composable get() = Brush.linearGradient(listOf(Rf.PrimaryFixedDim, Rf.PrimaryFixed))
