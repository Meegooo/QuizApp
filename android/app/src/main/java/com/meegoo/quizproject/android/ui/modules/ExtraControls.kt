package com.meegoo.quizproject.android.ui.modules

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.ui.ExtraColors
import com.meegoo.quizproject.android.ui.extraColors
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.buttons
import com.vanpra.composematerialdialogs.input
import com.vanpra.composematerialdialogs.title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.jvm.internal.Intrinsics

@Composable
fun CheckboxWithText(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: (Boolean) -> Unit = {}) {
    Row(
        Modifier
            .selectable(selected = selected,
                onClick = {
                    onClick(!selected)
                }
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected,
            onCheckedChange = {
                onClick(it)
            })
        Text(
            text = text,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
fun RadioWithText(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Row(
        Modifier
            .selectable(selected = selected,
                onClick = {
                    onClick()
                }
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick() }
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
fun SmallTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    backgroundColor: Color = Color.Transparent
) {
    BasicTextField(
        value = value,
        textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .then(modifier)
            .background(backgroundColor, shape = RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp),
        onValueChange = onValueChange,
        keyboardActions = keyboardActions,
        singleLine = true,
        decorationBox = {
            Box(
                modifier = Modifier
                    .padding(2.dp), Alignment.CenterStart
            ) {
                it()
            }
        }
    )
}

@Composable
fun NumericTextField(
    value: String,
    modifier: Modifier = Modifier,
    label: String = "",
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        isError = isError,
        enabled = enabled,
        singleLine = true,
        keyboardActions = keyboardActions,
        onValueChange = {
            val newValue = it.replace(",", ".").trim()
            if (newValue.isEmpty() || newValue == "-" || newValue.toDoubleOrNull() != null) {
                onValueChange(newValue)
            }
        }
    )
}


@Composable
fun OutlinedNumericTextField(
    value: String,
    modifier: Modifier = Modifier,
    label: String,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onValueChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        isError = isError,
        singleLine = true,
        keyboardActions = keyboardActions,
        onValueChange = {
            val newValue = it.replace(",", ".").trim()
            if (newValue.isEmpty() || newValue == "-" || newValue.toDoubleOrNull() != null) {
                onValueChange(newValue)
            }
        }
    )
}

@Composable
fun ExposedDropdownMenu(
    options: List<String>,
    selected: Int,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val icon = if (expanded) Icons.Filled.ArrowDropUp
    else Icons.Filled.ArrowDropDown

    Box(
        modifier
            .width(IntrinsicSize.Min)
            .height(60.dp)
    ) {
        Column {
            OutlinedTextField(
                value = options[selected],
                onValueChange = { },
                modifier = Modifier.fillMaxHeight(),
                label = { Text(label) },
                trailingIcon = {
                    Icon(icon, "")
                },
                readOnly = true,
                enabled = enabled
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { label ->
                    DropdownMenuItem(onClick = {
                        onClick(options.indexOf(label))
                        expanded = false
                    }) {
                        Text(text = label)
                    }
                }
            }
        }
        if (enabled)
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable {
                        expanded = !expanded
                    }
                    .background(Color.Transparent)
            )
    }
}

@Composable
fun MultiFloatingActionButton(
    fabIcon: ImageVector,
    items: List<MultiFabItem>,
    expanded: Boolean,
    showLabels: Boolean = true,
    stateChanged: (fabstate: Boolean) -> Unit,
    onFabItemClicked: (item: MultiFabItem) -> Unit
) {
    val transition: Transition<Boolean> = updateTransition(targetState = expanded)
    val scale: Float by transition.animateFloat(label = "fab scale animation") { isExpanded ->
        if (isExpanded) 56f else 0f
    }
    val alpha: Float by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 50)
        },
        label = "fab alpha animation"
    ) { isExpanded ->
        if (isExpanded) 1f else 0f
    }
    val shadow: Dp by transition.animateDp(
        transitionSpec = {
            tween(durationMillis = 50)
        },
        label = "fab shadow animation"
    ) { isExpanded ->
        if (isExpanded) 2.dp else 0.dp
    }
    val rotation: Float by transition.animateFloat(label = "fab rotation animation") { isExpanded ->
        if (isExpanded) 45f else 0f
    }
    Column(horizontalAlignment = Alignment.End) {
        items.forEach { item ->
            MiniFabItem(item, alpha, shadow, scale, showLabels, onFabItemClicked)
            Spacer(modifier = Modifier.height(40.dp))
        }
        FloatingActionButton(onClick = {
            stateChanged(transition.currentState != true)
        }) {
            Icon(
                imageVector = fabIcon,
                modifier = Modifier.rotate(rotation),
                contentDescription = "Add quiz or course"
            )
        }
    }
}

@Composable
private fun MiniFabItem(
    item: MultiFabItem,
    alpha: Float,
    shadow: Dp,
    scale: Float,
    showLabel: Boolean,
    onFabItemClicked: (item: MultiFabItem) -> Unit
) {
    val fabColor = MaterialTheme.colors.secondary
    val shadowColor = MaterialTheme.extraColors.backgroundOverlay
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        if (showLabel) {
            Text(
                item.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(animateFloatAsState(alpha).value)
                    .shadow(animateDpAsState(shadow).value)
                    .background(color = MaterialTheme.colors.surface)
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        val icon = ImageBitmap.imageResource(item.icon)

        Canvas(
            modifier = Modifier
                .size(32.dp)
                .clickable(
                    onClick = { onFabItemClicked(item) },
                    indication = rememberRipple(
                        bounded = false,
                        radius = 20.dp,
                        color = MaterialTheme.colors.onSecondary
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            drawCircle(
                Color(shadowColor.toArgb()),
                center = Offset(this.center.x + 2f, this.center.y + 7f),
                radius = scale
            )
            drawCircle(color = fabColor, scale)
            drawImage(
                icon,
                topLeft = Offset(
                    (this.center.x) - (icon.width / 2),
                    (this.center.y) - (icon.width / 2)
                ),
                alpha = alpha
            )
        }
    }
}

class MultiFabItem(
    val identifier: String,
    @DrawableRes val icon: Int,
    val label: String
)


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun namePrompt(
    title: String,
    onSubmit: suspend (String) -> String?,
): MaterialDialog {

    val enteredText = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    val dialog = remember {
        MaterialDialog(autoDismiss = false, onCloseRequest = {
            enteredText.value = ""
            it.hide()
        })
    }
    dialog.build {
        title(text = "Enter $title")
        input(label = title.replaceFirstChar { it.uppercaseChar() }, errorMessage = error.value) { inputString ->
            enteredText.value = inputString
        }
        buttons {
            positiveButton("Ok", onClick = {
                val result = runBlocking {
                    onSubmit(enteredText.value)
                }
                if (result == null) {
                    enteredText.value = ""
                    error.value = ""
                    dialog.hide()

                } else {
                    error.value = result
                    QuizApplication.showSortToast(error.value)
                }
            }
            )

            negativeButton("Cancel") {
                enteredText.value = ""
                error.value = ""
                dialog.hide()
            }
        }
    }
    return dialog
}