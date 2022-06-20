package com.meegoo.quizproject.android.ui.modules

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
@Composable
fun QuizListScroller(
    questions: List<QuestionDto>,
    selectedIndex: Int,
    onElementChanged: suspend (index: Int) -> Unit = {},
    colorMapper: (@Composable (index: Int, item: QuestionDto) -> Color)? = null,
    extrasAfter: LazyListScope.() -> Unit = {},
    cardContents: @Composable (index: Int, item: QuestionDto) -> Unit
) {

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(10.dp),
        state = listState,
        horizontalArrangement = Arrangement.Center
    ) {
        itemsIndexed(questions) { index, item ->
            Card(
                onClick = {
                    if (selectedIndex != index) {
                        scope.launch {
                            onElementChanged(index)
                        }
                    }
                },
                modifier = Modifier
                    .height(60.dp)
                    .width(60.dp)
                    .padding(5.dp),
                backgroundColor = if (colorMapper == null) {
                    MaterialTheme.colors.surface
                } else {
                    colorMapper(index, item)
                },
                elevation = 8.dp

            ) {
                cardContents(index, item)
            }
        }

        extrasAfter()
    }
}

@Composable
fun SingleAnswerCard(
    answers: List<QuestionDto.AnswerDto>,
    selectedIndex: List<Int> = emptyList(),
    colorMapper: @Composable (element: QuestionDto.AnswerDto) -> Color = { Color.Transparent },
    onAnswerChange: (idx: List<Int>) -> Unit = {}
) {
    Column(Modifier.fillMaxWidth()) {
        answers.forEachIndexed { idx, element ->
            RadioWithText(text = element.text, selected = (idx in selectedIndex),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(colorMapper(element)),
                onClick = {
                    if (idx !in selectedIndex) {
                        onAnswerChange(listOf(idx))
                    }
                }
            )
        }
    }
}


@Composable
fun MultipleAnswerCard(
    answers: List<QuestionDto.AnswerDto>,
    selectedIndex: List<Int> = emptyList(),
    colorMapper: @Composable (element: QuestionDto.AnswerDto) -> Color = { Color.Transparent },
    onAnswerChange: (idxs: List<Int>) -> Unit = {}
) {
    Column {
        answers.forEachIndexed { idx, element ->
            CheckboxWithText(text = element.text, selected = (idx in selectedIndex),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(colorMapper(element)),
                onClick = {
                    val copy = ArrayList(selectedIndex)
                    if (idx in copy) copy.remove(idx)
                    else copy.add(idx)
                    onAnswerChange(copy)
                }
            )
        }
    }
}

@Composable
fun TextFieldCard(
    userAnswer: String? = null,
    onAnswerChange: (String) -> Unit = {},
    systemAnswer: String? = null,
    isNumeric: Boolean = false,
    userAnswerColor: Color? = null,
    isEnabled: Boolean = true,

    ) {
    val keyboardOptions = if (isNumeric) {
        KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        )
    } else {
        KeyboardOptions.Default
    }
    if (userAnswer != null) {
        val modifier = if (userAnswerColor == null) {
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
        } else {
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .background(userAnswerColor)
        }

        Text("User answer", Modifier.padding(horizontal = 16.dp))
        TextField(
            value = userAnswer, onValueChange = onAnswerChange,
            keyboardOptions = keyboardOptions,
            enabled = isEnabled,
            modifier = modifier
        )
    }

    if (systemAnswer != null) {
        Text("Correct answer", Modifier.padding(horizontal = 16.dp))
        TextField(
            value = systemAnswer,
            onValueChange = { },
            enabled = false,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
        )
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QuizCard(
    onQuizClick: (uuid: UUID) -> Unit,
    quiz: QuizDto,
    quizAttempt: QuizAttemptDto?,
    editing: Boolean,
    loadAttempt: suspend (quizUuid: UUID, force: Boolean) -> Unit = { _, _ -> },
    permissionStartsAt:GrantedPermission = GrantedPermission.WRITE
) {

    val scope = rememberCoroutineScope()
    var expired by remember(quiz.id) { mutableStateOf(false) }
    LaunchedEffect(quiz.id, expired) {
        loadAttempt(quiz.id!!, expired)
    }
    Card(
        elevation = 2.dp,
        modifier = Modifier
            .padding(PaddingValues(10.dp, 10.dp))
            .fillMaxWidth(1.0f)
            .height(80.dp)
            .background(MaterialTheme.colors.surface),
        onClick = {
            onQuizClick(quiz.id!!)
        }
    ) {
        Column(Modifier.padding(16.dp, 8.dp)) {
            Text(
                quiz.name!!,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f)
            )

            if (editing) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        if (quiz.publishedAt != null) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = "Quiz published",
                                tint = MaterialTheme.extraColors.green,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = "Quiz not published",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text("${quiz.questionCount ?: 0} Questions", Modifier.padding(start = 5.dp))
                    }

                    PermissionWindow(quiz.permissions, startAt = permissionStartsAt)
                }
            } else {
                Row {
                    if (quizAttempt != null) {
                        if (quizAttempt.closed == true) {
                            Icon(
                                imageVector = Icons.Filled.CheckBox,
                                contentDescription = "Quiz finished",
                                tint = MaterialTheme.extraColors.green,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                String.format("%.2f/%.2f", quizAttempt.score, quiz.score!!),
                                Modifier.padding(start = 5.dp)
                            )
                        } else if (quizAttempt.startedAt != null) {
                            val timeFormatted = timeRemaining(quizAttempt.timeRemaining, quiz.id) {
                                scope.launch {
                                    delay(4000)
                                    expired = true
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.WatchLater,
                                contentDescription = "Quiz active",
                                tint = MaterialTheme.colors.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(timeFormatted, Modifier.padding(start = 5.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckBoxOutlineBlank,
                                contentDescription = "Quiz finished",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("${quiz.questionCount ?: 0} Questions", Modifier.padding(start = 5.dp))
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun timeRemaining(timeRemaining: Long?, key: Any?, onFinish: () -> Unit = {}): String {
    val scope = rememberCoroutineScope()
    return timeRemaining?.let { safeTime ->
        var time by remember(key) { mutableStateOf(safeTime) }
        DisposableEffect(key) {
            Log.d("TimeRemaining", "Started $time")
            val job = scope.launch {
                while (time > 0 && isActive) {
                    delay(1000)
                    time--
                }
                if (time <= 0) {
                    Log.d("TimeRemaining", "Finishing")
                    onFinish()
                    Log.d("TimeRemaining", "Done")
                }
            }
            onDispose {
                job.cancel()
            }
        }


        val seconds = time % 60
        val minutes = time / 60 % 60
        val hours = time / 3600
        String.format("%02d:%02d:%02d", hours, minutes, seconds)

    } ?: "Unlimited"
}

@Composable
fun PermissionWindow(
    permissions: List<GrantedPermission>,
    startAt: GrantedPermission = GrantedPermission.WRITE
) {
    val height = 25.dp
    val width = 20.dp

    val boxWidth = if (permissions.contains(GrantedPermission.OWNER)) {
        80.dp
    } else {
        width * (5 - startAt.level)
    }
    Box(
        Modifier
            .background(MaterialTheme.colors.surface, RectangleShape)
            .size(boxWidth, height)
    ) {
        if (permissions.contains(GrantedPermission.OWNER)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width*4, height)) {
                Text("OWNER", color = MaterialTheme.extraColors.red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            val toRender = GrantedPermission.values().sortedBy { it.level }
                .filter { startAt.level <= it.level && it.level <= GrantedPermission.ADMINISTRATION.level }
            var xOffset = 0.dp
            for (grantedPermission in toRender) {
                val color = if (grantedPermission !in permissions) {
                    MaterialTheme.extraColors.disabledText
                } else {
                    when (grantedPermission) {
                        GrantedPermission.READ -> MaterialTheme.extraColors.green
                        GrantedPermission.SHARE -> MaterialTheme.extraColors.blue
                        GrantedPermission.WRITE -> MaterialTheme.extraColors.purple
                        GrantedPermission.ADMINISTRATION -> MaterialTheme.extraColors.red
                        else -> MaterialTheme.extraColors.disabledText
                    }
                }
                val letter = grantedPermission.name[0].toString()
                Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(xOffset).size(width, height)) {
                    Text(letter, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                xOffset += width
            }
        }

    }
}