package com.thebackendguy.myandroidtestapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebackendguy.myandroidtestapp.ui.icons.LIcon
import com.thebackendguy.myandroidtestapp.ui.icons.Lucide
import com.thebackendguy.myandroidtestapp.ui.icons.LucideIcon
import com.thebackendguy.myandroidtestapp.ui.theme.Rf
import com.thebackendguy.myandroidtestapp.ui.theme.RfType

/**
 * The web's phone TextField: label above a white 48dp box with a leading icon,
 * a focus ring, and a hint or error line underneath.
 */
@Composable
fun RfTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: LucideIcon,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    onImeAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    prefix: String? = null,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    var visible by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val ring = when {
        error != null -> Rf.Error
        focused -> Rf.Primary.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = RfType.LabelMd, color = Rf.OnSurface)
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .softShadow(shape)
                .clip(shape)
                .background(Rf.Lowest)
                .border(if (error != null) 1.5.dp else 2.dp, ring, shape)
                .padding(start = 14.dp, end = if (isPassword || trailing != null) 4.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LIcon(icon, size = 20.dp, tint = Rf.Outline, strokeWidth = 1.75f)
            if (prefix != null) {
                Text(
                    text = prefix,
                    style = RfType.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                    color = Rf.OnSurface,
                    modifier = Modifier.padding(start = 10.dp)
                )
                Box(Modifier.padding(start = 10.dp).size(width = 1.dp, height = 20.dp).background(Rf.OutlineVariant))
            }
            Box(Modifier.weight(1f).padding(start = 10.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = RfType.BodyMd, color = Rf.Outline.copy(alpha = 0.8f), maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    interactionSource = source,
                    textStyle = RfType.BodyMd.copy(color = Rf.OnSurface, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(Rf.Primary),
                    visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                        imeAction = imeAction,
                        capitalization = capitalization
                    ),
                    keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                Box(
                    Modifier.size(44.dp).pressable({ visible = !visible }),
                    contentAlignment = Alignment.Center
                ) {
                    LIcon(if (visible) Lucide.Eye else Lucide.EyeOff, size = 20.dp, tint = Rf.Outline, strokeWidth = 1.75f,
                        contentDescription = if (visible) "Hide password" else "Show password")
                }
            }
            trailing?.invoke()
        }
        val message = error ?: hint
        if (message != null) {
            Text(
                text = message,
                style = RfType.BodySm.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = if (error != null) Rf.Error else Rf.Outline
            )
        }
    }
}
