package com.meegoo.quizproject.android.ui.screens

import android.R.attr
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.ui.viewmodels.QuizEditViewModel
import com.meegoo.quizproject.android.ui.viewmodels.TimeLimit
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.vanpra.composematerialdialogs.*
import java.util.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.meegoo.quizproject.android.network.api.RequestController
import com.meegoo.quizproject.android.ui.modules.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuizEditorScreen(
    mainViewModel: MainViewModel,
    quizEditViewModel: QuizEditViewModel,
    quizUuid: String,
    navigateToQuestionEdit: (UUID) -> Unit,
    navigateToAccessControl: (UUID) -> Unit
) {
    val newQuiz = quizUuid == "new"
    val quiz = remember {
        if (newQuiz) {
            mutableStateOf(QuizDto())
        } else {
            val uuid = UUID.fromString(quizUuid)
            mainViewModel.loadQuizEditable(uuid)
        }
    }


    val clipboardManager = LocalClipboardManager.current
    val navController = AppNavController.current
    val dialog = MaterialDialog(autoDismiss = false)
    dialog.build {
        title(text = "Confirm import")
        message(text = "Pressing OK will read quiz data from your clipboard")
        buttons {
            positiveButton("OK", onClick = {
                val result = runBlocking {
                    val text = clipboardManager.getText()?.toString()
                    if (text == null) {
                        "Malformed data"
                    } else {
                        val dto: QuizDto? = withContext(Dispatchers.IO) {
                            try {
                                RequestController.objectMapper.readValue(
                                    text,
                                    QuizDto::class.java
                                )
                            } catch (e: JsonProcessingException) {
                                null
                            } catch (e: JsonMappingException) {
                                null
                            }
                        }
                        when {
                            dto == null -> "Malformed data"
                            mainViewModel.importQuiz(dto) != null -> null
                            else -> "Malformed data or connection error"
                        }
                    }
                }
                if (result == null) {
                    dialog.hide()
                    navController.popBackStack()
                } else {
                    QuizApplication.showSortToast(result)
                }
            }
            )


            negativeButton("Cancel")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz.value?.name ?: "New Quiz") },
                actions = {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopStart)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (quiz.value?.id != null) {
                                DropdownMenuItem(onClick = {
                                    mainViewModel.launch {
                                        expanded = false
                                        val exportQuiz = mainViewModel.exportQuiz(quiz.value?.id!!)
                                        if (exportQuiz != null) {
                                            val text = withContext(Dispatchers.IO) {
                                                RequestController.objectMapper.writeValueAsString(exportQuiz)
                                            }
                                            clipboardManager.setText(AnnotatedString(text))
                                            QuizApplication.showSortToast("Quiz data copied to clipboard")
                                        } else {
                                            QuizApplication.showSortToast("Connection error")
                                        }
                                    }
                                }) {
                                    Text("Export")
                                }
                            } else {
                                DropdownMenuItem(onClick = {
                                    expanded = false
                                    dialog.show()
                                }) {
                                    Text("Import")
                                }
                            }
                        }

                    }

                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back button"
                        )
                    }
                },
            )
        })
    {
        //Loading quiz error
        if (!newQuiz && mainViewModel.errors.loadQuizError.value != null) {
            ConnectionErrorModule {
                mainViewModel.loadQuizEditable(UUID.fromString(quizUuid), true)
            }
        } else if (quiz.value == null || mainViewModel.loadingQuiz.value) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.padding(10.dp))
            }
        } else {
            LaunchedEffect(quiz.value) {
                quizEditViewModel.fromQuizDto(quiz.value!!)
            }
            val deleteDialog = remember { MaterialDialog() }

            deleteDialog.build {
                title(text = "Confirm deletion")
                message(text = "Please enter quiz name to confirm deletion")
                input(
                    label = "Quiz Name",
                    hint = quizEditViewModel.name.value,
                    waitForPositiveButton = false
                ) { inputString ->
                    if (inputString == quizEditViewModel.name.value) {
                        enablePositiveButton()
                    } else {
                        disablePositiveButton()
                    }
                }
                buttons {
                    positiveButton("Delete", onClick = {
                        mainViewModel.launch {
                            quizEditViewModel.deleteQuiz(mainViewModel)
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
                    .padding(it)
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
            ) {
//
                Spacer(Modifier.height(20.dp))
                TextField(value = quizEditViewModel.name.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp),
                    label = { Text("Name") },

                    onValueChange = { newValue ->
                        quizEditViewModel.name.value = newValue
                    })
                Spacer(Modifier.height(20.dp))

                //Time Limit
                TimerPicker(
                    time = quizEditViewModel.timeLimit.value,
                    isUnlimited = quizEditViewModel.timeUnlimited.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp),
                    label = { Text("Time Limit") }
                ) {
                    quizEditViewModel.timeLimit.value = it
                }

                Spacer(Modifier.height(10.dp))
                CheckboxWithText(text = "Unlimited",
                    selected = quizEditViewModel.timeUnlimited.value,
                    modifier = Modifier
                        .padding(10.dp, 0.dp)
                        .fillMaxWidth(),
                    onClick = {
                        quizEditViewModel.timeUnlimited.value = !quizEditViewModel.timeUnlimited.value
                    }
                )
                Spacer(Modifier.height(20.dp))

                //Score
                val focusManager = LocalFocusManager.current
                NumericTextField(
                    value = quizEditViewModel.score.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    label = "Score",
                    enabled = !quizEditViewModel.automaticScore.value,
                    onValueChange = {
                        quizEditViewModel.score.value = it
                    },

                    )
                Spacer(Modifier.height(10.dp))
                CheckboxWithText(text = "Automatic score",
                    selected = quizEditViewModel.automaticScore.value,
                    modifier = Modifier
                        .padding(10.dp, 0.dp)
                        .fillMaxWidth(),
                    onClick = {
                        quizEditViewModel.automaticScore.value = !quizEditViewModel.automaticScore.value
                    }
                )
                Spacer(Modifier.height(20.dp))
                Divider(color = MaterialTheme.extraColors.divider)

                //Confirmation buttons
                ConfirmationButtons(
                    newQuiz,
                    mainViewModel,
                    quizEditViewModel,
                    navigateToQuestionEdit,
                    navigateToAccessControl,
                    deleteDialog
                )

            }

            if (quizEditViewModel.updatingQuiz.value > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.extraColors.backgroundOverlay)
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ConfirmationButtons(
    newQuiz: Boolean,
    mainViewModel: MainViewModel,
    quizEditViewModel: QuizEditViewModel,
    navigateToQuestionEdit: (UUID) -> Unit,
    navigateToAccessControl: (UUID) -> Unit,
    deleteDialog: MaterialDialog
) {
    val navController = AppNavController.current
    Row(
        Modifier
            .padding(10.dp)
            .padding(top = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        if (newQuiz) {
            Button(
                onClick = {
                    mainViewModel.launch {
                        quizEditViewModel.createQuiz(mainViewModel, navigateToQuestionEdit)
                    }
                },
                enabled = quizEditViewModel.name.value.isNotBlank(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Create")
            }
        } else {
            Button(
                onClick = {
                    navigateToQuestionEdit(quizEditViewModel.id.value!!)
                },
                modifier = Modifier.width(150.dp)
            ) {
                Text("Edit Questions")
            }
            Button(
                onClick = {
                    mainViewModel.launch {
                        quizEditViewModel.updateQuiz(mainViewModel)
                        if (quizEditViewModel.updatingQuiz.value == 0) {
                            QuizApplication.showSortToast("Quiz updated")
                        }
                    }
                },
                enabled = quizEditViewModel.name.value.isNotBlank(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Confirm")
            }
        }
    }

    if (quizEditViewModel.publishedAt.value == null && !newQuiz) {
        Row(
            Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                    mainViewModel.launch {
                        quizEditViewModel.publishQuiz(mainViewModel)
                    }
                },
                modifier = Modifier.width(150.dp)
            ) {
                Text("Publish")
            }
        }
    } else {
        if (quizEditViewModel.permissions.contains(GrantedPermission.ADMINISTRATION)) {
            Row(
                Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        navigateToAccessControl(quizEditViewModel.id.value!!)
                    },
                    modifier = Modifier.width(150.dp)
                ) {
                    Text("Access Control")
                }
            }
        }
    }

    if (!newQuiz) {
        Row(
            Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            LongPressButton(
                onClick = { QuizApplication.showSortToast("Hold to delete") },
                onLongClick = {
                    mainViewModel.launch {
                        QuizApplication.vibrate(50)
                        deleteDialog.show()
                    }
                },
                modifier = Modifier.width(150.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun TimerPicker(
    time: TimeLimit,
    isUnlimited: Boolean,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit = {},
    onTimeChange: (time: TimeLimit) -> Unit
) {

    val textFieldValue = if (isUnlimited) {
        TextFieldValue("Unlimited", selection = TextRange("".length))
    } else {
        TextFieldValue(time.toString(), selection = TextRange(time.toString().length))
    }

    val focusManager = LocalFocusManager.current
    TextField(
        value = textFieldValue,
        onValueChange = { value ->
            if (isUnlimited) return@TextField
            val intValue = value.text.replace("[^\\d]+".toRegex(), "").toInt()
            val seconds = (intValue % 100)
            val minutes = (intValue / 100 % 100)
            val hours = (intValue / 10000).coerceAtMost(99)
            val localTime = TimeLimit(hours, minutes, seconds)
            onTimeChange(localTime)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        label = label,
        readOnly = isUnlimited,
        modifier = modifier
            .onFocusChanged {
                if (!it.hasFocus) {
                    onTimeChange(time.toProperTime())
                }
            }
    )
}