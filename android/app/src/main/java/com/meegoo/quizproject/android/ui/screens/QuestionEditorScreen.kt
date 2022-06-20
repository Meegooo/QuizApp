package com.meegoo.quizproject.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.imePadding
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.ui.viewmodels.QuestionEditViewModel
import com.meegoo.quizproject.android.ui.viewmodels.QuizEditViewModel
import com.meegoo.quizproject.android.ui.viewmodels.TimeLimit
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*


@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun QuestionEditorScreen(
    quizEditViewModel: QuizEditViewModel,
    questionUuid: String,
) {
    val coroutineScope = rememberCoroutineScope()
    val questionEditViewModel = viewModel<QuestionEditViewModel>()
    LaunchedEffect(questionUuid) {
        if (questionUuid != "new") {
            val uuid = UUID.fromString(questionUuid)
            quizEditViewModel.questions.find { it.id == uuid }?.also {
                questionEditViewModel.fromQuestionDto(it)
            }
        }
    }

    val newQuestion = questionUuid == "new"

    val focusManager = LocalFocusManager.current
    //Question Type Selector
    val questionTypeDialog = remember { MaterialDialog() }
    questionTypeDialog.build {
        title(text = "Question type")
        listItems(
            listOf("Single Answer", "Multiple Answer", "Text Answer", "Number Answer"),
            closeOnClick = false
        ) { index, _ ->

            val questionType = QuestionDto.QuestionType.values()[index]
            if (questionEditViewModel.questionType.value != questionType) {
                questionEditViewModel.questionType.value = questionType
                questionEditViewModel.answers.clear()
            }
            coroutineScope.launch {
                delay(50)
                hide()
            }

        }
    }

    LaunchedEffect(questionUuid) {
        if (questionEditViewModel.questionType.value == null) {
            questionTypeDialog.show()
        }
    }


    val navController = AppNavController.current
    Scaffold(
        topBar = {
            val type = when (questionEditViewModel.questionType.value) {
                QuestionDto.QuestionType.SINGLE_ANSWER -> "single answer"
                QuestionDto.QuestionType.MULTIPLE_ANSWER -> "multiple answer"
                QuestionDto.QuestionType.TEXT_FIELD -> "text answer"
                QuestionDto.QuestionType.NUMERIC_FIELD -> "number answer"
                else -> ""
            }
            TopAppBar(
                title = {
                    if (newQuestion) {
                        Text("New $type question")

                    } else {
                        Text("Editing $type question")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back button"
                        )
                    }
                },
            )
        }) { paddingValues ->
        val deleteDialog = remember { MaterialDialog() }
        deleteDialog.build {
            title(text = "Confirm deletion")
            message(text = "Please confirm deletion")
            buttons {
                positiveButton("Delete", onClick = {
                    quizEditViewModel.deleteQuestion(questionEditViewModel.id.value)
                    coroutineScope.launch {
                        delay(100)
                        navController.popBackStack()
                    }
                })

                negativeButton("Cancel")
            }
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
        ) {

            //Name
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(10.dp, 10.dp, 10.dp, 0.dp)
                    .fillMaxWidth()
            ) {
                Button(onClick = {
                    questionEditViewModel.previewingQuestion.value =
                        !questionEditViewModel.previewingQuestion.value

                }) {
                    Text("Preview")
                }

                IconButton(onClick = {
                    questionEditViewModel.showingMarkdownHelp.value = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        tint = MaterialTheme.colors.onBackground,
                        contentDescription = "Markdown Help"
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (questionEditViewModel.previewingQuestion.value) {

                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 10.dp, 10.dp, 0.dp)
                ) {

                    MarkdownText(
                        text = questionEditViewModel.question.value,
                        Modifier.padding(5.dp, 5.dp, 5.dp, 20.dp)
                    )
                }
            } else {
                MarkdownEditor(
                    text = questionEditViewModel.question.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp),
                    onTextChange = { newValue ->
                        questionEditViewModel.question.value = newValue
                    },
                    label = "Question"
                )
            }

            Spacer(Modifier.height(20.dp))

            NumericTextField(value = questionEditViewModel.weight.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 0.dp),
                label = "Weight",
                isError = (questionEditViewModel.weight.value.toDoubleOrNull() ?: -1.0) <= 0.0,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChange = {
                    if (!it.startsWith("-")) {
                        questionEditViewModel.weight.value = it
                    } else {
                        questionEditViewModel.weight.value = it.substring(1)
                    }
                }
            )

            if (questionEditViewModel.questionType.value == QuestionDto.QuestionType.TEXT_FIELD) {
                Spacer(Modifier.height(20.dp))
                CheckboxWithText(
                    text = "Trim padding",
                    selected = questionEditViewModel.trimPadding.value,
                    onClick = {
                        questionEditViewModel.trimPadding.value = it
                    },
                    modifier = Modifier
                        .padding(10.dp, 0.dp)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))
                CheckboxWithText(
                    text = "Ignore case",
                    selected = questionEditViewModel.ignoreCase.value,
                    onClick = {
                        questionEditViewModel.ignoreCase.value = it
                    },
                    modifier = Modifier
                        .padding(10.dp, 0.dp)
                        .fillMaxWidth()
                )
            } else if (questionEditViewModel.questionType.value == QuestionDto.QuestionType.NUMERIC_FIELD) {
                Spacer(Modifier.height(20.dp))
                TextField(
                    value = questionEditViewModel.numericPrecision.value.toString(),
                    onValueChange = { value ->
                        val intOrNull = if (value.isBlank()) 0 else value.toIntOrNull()
                        if (intOrNull != null) {
                            questionEditViewModel.numericPrecision.value = intOrNull
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    label = { Text("Numeric Precision") },
                    modifier = Modifier
                        .padding(10.dp, 0.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            CheckboxWithText(
                text = "Advanced scoring",
                selected = questionEditViewModel.advancedScoring.value,
                onClick = {
                    questionEditViewModel.advancedScoring.value =
                        !questionEditViewModel.advancedScoring.value
                    if (!questionEditViewModel.advancedScoring.value) {
                        for (index in 0 until questionEditViewModel.answers.size) {
                            val answer = questionEditViewModel.answers[index]
                            answer.notChosenScoreAsString = "0.0"
                            when (questionEditViewModel.questionType.value) {
                                QuestionDto.QuestionType.SINGLE_ANSWER -> {
                                    answer.chosenScoreAsString = if (index == 0) "1.0" else "0.0"
                                }
                                QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                                    answer.chosenScoreAsString = "0.0"
                                    answer.notChosenScoreAsString = "1.0"
                                }
                                QuestionDto.QuestionType.TEXT_FIELD,
                                QuestionDto.QuestionType.NUMERIC_FIELD -> {
                                    answer.chosenScoreAsString = "1.0"
                                }
                            }
                        }
                        questionEditViewModel.baseScore.value = "0.0"
                        questionEditViewModel.maxScore.value = "1.0"
                        if (questionEditViewModel.questionType.value == QuestionDto.QuestionType.MULTIPLE_ANSWER) {
                            questionEditViewModel.maxScore.value = questionEditViewModel.answers.size.toDouble().toString()
                        }
                    }

                },
                modifier = Modifier
                    .padding(10.dp, 0.dp)
                    .fillMaxWidth()
            )

            AnimatedVisibility(
                questionEditViewModel.advancedScoring.value,
                enter = expandVertically(Alignment.Top),
                exit = shrinkVertically(Alignment.Top)
            ) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    OutlinedNumericTextField(
                        questionEditViewModel.baseScore.value,
                        modifier = Modifier.weight(1.0f),
                        label = "Base Score",
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        onValueChange = {
                            questionEditViewModel.baseScore.value = it
                        })

                    Spacer(Modifier.width(30.dp))
                    OutlinedNumericTextField(
                        questionEditViewModel.maxScore.value,
                        modifier = Modifier.weight(1.0f),
                        isError = (questionEditViewModel.maxScore.value.toDoubleOrNull() ?: -1.0) <= 0.0,
                        label = "Max Score",
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        onValueChange = {
                            if (!it.startsWith("-")) {
                                questionEditViewModel.maxScore.value = it
                            } else {
                                questionEditViewModel.maxScore.value = it.substring(1)
                            }

                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = MaterialTheme.extraColors.divider)

            when (questionEditViewModel.questionType.value) {
                QuestionDto.QuestionType.SINGLE_ANSWER -> {
                    SingleAnswerCard(questionEditViewModel)
                }
                QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                    MultipleAnswerCard(questionEditViewModel)
                }
                QuestionDto.QuestionType.TEXT_FIELD -> {
                    TextAnswerCard(questionEditViewModel, false)
                }
                QuestionDto.QuestionType.NUMERIC_FIELD -> {
                    TextAnswerCard(questionEditViewModel, true)
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = MaterialTheme.extraColors.divider)

            //Confirmation buttons
            ConfirmationButtons(newQuestion,
                confirmEnabled = questionEditViewModel.answers.isNotEmpty() &&
                        ((questionEditViewModel.maxScore.value.toDoubleOrNull() ?: -1.0) > 0 || !questionEditViewModel.advancedScoring.value) &&
                        (questionEditViewModel.weight.value.toDoubleOrNull() ?: -1.0) > 0,
                onDelete = {
                    deleteDialog.show()
                },
                onConfirm = {
                    quizEditViewModel.saveQuestion(questionEditViewModel)
                    coroutineScope.launch {
                        delay(100)
                        navController.popBackStack()
                    }
                }
            )
        }

        MarkdownHelp(questionEditViewModel.showingMarkdownHelp.value) {
            questionEditViewModel.showingMarkdownHelp.value = false
        }
        if (questionEditViewModel.showingMarkdownHelp.value) {
            BackHandler {
                questionEditViewModel.showingMarkdownHelp.value = false
            }
        }


    }
}

@Composable
private fun ConfirmationButtons(
    newQuestion: Boolean,
    confirmEnabled: Boolean = true,
    onDelete: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    Row(
        Modifier
            .padding(10.dp)
            .padding(top = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        if (!newQuestion) {
            LongPressButton(
                onClick = { QuizApplication.showSortToast("Hold to delete") },
                onLongClick = {
                    onDelete()
                    QuizApplication.vibrate(50)
                },
                modifier = Modifier.width(150.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("Delete")
            }
        }

        Button(
            onClick = {
                onConfirm()
            },
            enabled = confirmEnabled,
            modifier = Modifier.width(150.dp)
        ) {
            Text("Confirm")
        }

    }
}

@Composable
private fun RowScope.AnswerButtons(
    answer: QuestionDto.AnswerDto,
    index: Int,
    answerSize: Int,
    focusManager: FocusManager,
    onAnswerDelete: (QuestionDto.AnswerDto) -> Unit,
    onAnswerUp: (idx: Int) -> Unit
) {
    val height = 30.dp
    LongPressIconButton(
        onClick = { QuizApplication.showSortToast("Hold to delete") },
        onLongClick = {
            onAnswerDelete(answer)
            QuizApplication.vibrate(50)
        }) {
        Icon(
            imageVector = Icons.Filled.RemoveCircle,
            tint = MaterialTheme.colors.error,
            contentDescription = "Remove Answer"
        )
    }
    if (index != 0) {
        IconButton(modifier = Modifier.size(height),
            onClick = {
                focusManager.clearFocus()
                onAnswerUp(index)
            }) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Move Answer Up"
            )
        }
    } else {
        Spacer(Modifier.size(height))
    }


    if (index != answerSize - 1) {
        IconButton(modifier = Modifier.size(height),
            onClick = {
                onAnswerUp(index + 1)
                focusManager.clearFocus()
            }) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = "Move Answer Down"
            )
        }
    } else {
        Spacer(Modifier.size(height))
    }
}


@Composable
private fun SingleAnswerCard(
    questionEditViewModel: QuestionEditViewModel,
) {
    ChoiceAnswerCard(
        answers = questionEditViewModel.answers,
        isMultipleChoice = false,
        advancedScoring = questionEditViewModel.advancedScoring.value,
        onValueChange = { idx, element ->
            questionEditViewModel.answers[idx] = element
        },
        onAnswerAdd = {
            val chosenScore = if (questionEditViewModel.answers.isEmpty()) 1.0 else 0.0
            questionEditViewModel.answers.add(QuestionDto.AnswerDto("", chosenScore))
        },
        onAnswerDelete = {
            questionEditViewModel.answers.remove(it)
            if (!questionEditViewModel.advancedScoring.value && it.chosenScore == 1.0) {
                if (questionEditViewModel.answers.isNotEmpty()) {
                    questionEditViewModel.answers[0].chosenScoreAsString = "1.0"
                }
            }
        },
        onAnswerUp = {
            val toMoveUp = questionEditViewModel.answers[it]
            questionEditViewModel.answers[it] = questionEditViewModel.answers[it - 1]
            questionEditViewModel.answers[it - 1] = toMoveUp
        },

        elementEnabled = { idx, element ->
            RadioButton(
                selected = element.chosenScore == 1.0,
                onClick = {
                    if (element.chosenScore != 1.0) {
                        for (index in questionEditViewModel.answers.indices) {
                            val answer = questionEditViewModel.answers[index]
                            answer.chosenScoreAsString = "0.0"
                            questionEditViewModel.answers[idx] = answer
                        }
                        element.chosenScoreAsString = "1.0"
                        questionEditViewModel.answers[idx] = element
                    }
                },
            )
        },
        elementDisabled = { _, _ ->
            RadioButton(
                selected = false,
                onClick = { },
                enabled = false
            )
        })
}


@Composable
private fun MultipleAnswerCard(
    questionEditViewModel: QuestionEditViewModel,
) {
    ChoiceAnswerCard(answers = questionEditViewModel.answers,
        isMultipleChoice = true,
        advancedScoring = questionEditViewModel.advancedScoring.value,
        onValueChange = { idx, element ->
            questionEditViewModel.answers[idx] = element
        },
        onAnswerAdd = {
            questionEditViewModel.answers.add(QuestionDto.AnswerDto(text = "", chosenScore = 0.0, notChosenScore = 1.0))
        },
        onAnswerDelete = {
            questionEditViewModel.answers.remove(it)
        },
        onAnswerUp = {
            val toMoveUp = questionEditViewModel.answers[it]
            questionEditViewModel.answers[it] = questionEditViewModel.answers[it - 1]
            questionEditViewModel.answers[it - 1] = toMoveUp
        },
        elementEnabled = { idx, element ->
            Checkbox(
                checked = element.chosenScore == 1.0,
                onCheckedChange = {
                    if (it) {
                        element.chosenScoreAsString = "1.0"
                        element.notChosenScoreAsString = "0.0"
                    } else {
                        element.chosenScoreAsString = "0.0"
                        element.notChosenScoreAsString = "1.0"
                    }
                    questionEditViewModel.answers[idx] = element
                },
            )
        },
        elementDisabled = { _, _ ->
            Checkbox(
                checked = false,
                onCheckedChange = { },
                enabled = false
            )
        })
}


@Composable
private fun TextAnswerCard(
    questionEditViewModel: QuestionEditViewModel,
    isNumeric: Boolean
) {
    val keyboardOptions = if (isNumeric) {
        KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        )
    } else {
        KeyboardOptions.Default
    }

    val validateText: (String) -> String? = if (isNumeric) {
        {
            val newValue = it.replace(",", ".").trim()
            if (newValue.isEmpty() || newValue == "-" || newValue.toDoubleOrNull() != null) {
                newValue
            } else {
                null
            }
        }
    } else {
        { it }
    }

    TextAnswerCard(
        answers = questionEditViewModel.answers,
        advancedScoring = questionEditViewModel.advancedScoring.value,
        onValueChange = { idx, element ->
            questionEditViewModel.answers[idx] = element
        },
        onAnswerAdd = {
            questionEditViewModel.answers.add(QuestionDto.AnswerDto("", 1.0))
        },
        onAnswerDelete = {
            questionEditViewModel.answers.remove(it)
        },
        validateText = validateText,
        keyboardOptions = keyboardOptions
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ChoiceAnswerCard(
    answers: List<QuestionDto.AnswerDto>,
    isMultipleChoice: Boolean,
    advancedScoring: Boolean = false,
    onValueChange: (index: Int, QuestionDto.AnswerDto) -> Unit = { _, _ -> },
    onAnswerAdd: () -> Unit = {},
    onAnswerDelete: (QuestionDto.AnswerDto) -> Unit = {},
    onAnswerUp: (idx: Int) -> Unit = {},
    elementEnabled: @Composable (idx: Int, QuestionDto.AnswerDto) -> Unit = { _, _ -> },
    elementDisabled: @Composable (idx: Int, QuestionDto.AnswerDto) -> Unit = { _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    val height = 30.dp
    Column(Modifier.fillMaxWidth()) {
        answers.forEachIndexed { idx, element ->
            val visible = remember(idx) { MutableTransitionState(false) }
            visible.targetState = true
            AnimatedVisibility(
                visible,
                enter = expandVertically(Alignment.Top),
                exit = shrinkVertically(Alignment.Top)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {

                        if (advancedScoring) {
                            elementDisabled(idx, element)
                        } else {
                            elementEnabled(idx, element)
                        }

                        val background = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                        SmallTextField(
                            value = element.text, onValueChange = {
                                element.text = it
                                onValueChange(idx, element)
                            },
                            modifier = Modifier
                                .height(height)
                                .weight(1.0f),
                            backgroundColor = background,
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        //Icon buttons
                        AnswerButtons(
                            answer = element,
                            index = idx,
                            answerSize = answers.size,
                            focusManager = focusManager,
                            onAnswerDelete = {
                                onAnswerDelete(element)
                            },
                            onAnswerUp = onAnswerUp
                        )

                    }

                    if (advancedScoring) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {

                            OutlinedNumericTextField(
                                element.chosenScoreAsString,
                                modifier = Modifier.weight(1.0f),
                                label = "Selected Score",
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                onValueChange = {
                                    element.chosenScoreAsString = it
                                    onValueChange(idx, element)
                                })

                            if (isMultipleChoice) {
                                Spacer(Modifier.width(30.dp))

                                OutlinedNumericTextField(
                                    element.notChosenScoreAsString,
                                    modifier = Modifier.weight(1.0f),
                                    label = "Not Selected Score",
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    onValueChange = {
                                        element.notChosenScoreAsString = it
                                        onValueChange(idx, element)
                                    })

                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

        }
        IconButton(onClick = { onAnswerAdd() }, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(
                imageVector = Icons.Filled.Add,
                tint = MaterialTheme.colors.onBackground,
                contentDescription = "Add Answer"
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TextAnswerCard(
    answers: List<QuestionDto.AnswerDto>,
    advancedScoring: Boolean = false,
    onValueChange: (index: Int, QuestionDto.AnswerDto) -> Unit = { _, _ -> },
    onAnswerAdd: () -> Unit = {},
    onAnswerDelete: (QuestionDto.AnswerDto) -> Unit = {},
    validateText: (String) -> String? = { it },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val focusManager = LocalFocusManager.current
    Column(Modifier.fillMaxWidth()) {
        answers.forEachIndexed { idx, element ->
            val visible = remember(idx) { MutableTransitionState(false) }
            visible.targetState = true
            AnimatedVisibility(
                visible,
                enter = expandVertically(Alignment.Top),
                exit = shrinkVertically(Alignment.Top)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        TextField(
                            value = element.text,
                            label = { Text("Correct Answer") },
                            onValueChange = { value ->
                                validateText(value)?.let {
                                    element.text = it
                                    onValueChange(idx, element)
                                }
                            },
                            keyboardOptions = keyboardOptions,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .weight(2f)
                        )
                        //Icon buttons
                        LongPressIconButton(
                            onClick = { QuizApplication.showSortToast("Hold to delete") },
                            onLongClick = {
                                onAnswerDelete(element)
                                QuizApplication.vibrate(50)
                            }) {
                            Icon(
                                imageVector = Icons.Filled.RemoveCircle,
                                tint = MaterialTheme.colors.error,
                                contentDescription = "Remove Answer"
                            )
                        }
                    }

                    if (advancedScoring) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {

                            OutlinedNumericTextField(
                                element.chosenScoreAsString,
                                modifier = Modifier.weight(1.0f),
                                label = "Selected Score",
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                onValueChange = {
                                    element.chosenScoreAsString = it
                                    onValueChange(idx, element)
                                })
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

        }
        IconButton(onClick = { onAnswerAdd() }, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(
                imageVector = Icons.Filled.Add,
                tint = MaterialTheme.colors.onBackground,
                contentDescription = "Add Answer"
            )
        }
    }
}