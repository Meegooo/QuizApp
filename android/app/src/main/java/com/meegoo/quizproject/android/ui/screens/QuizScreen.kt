package com.meegoo.quizproject.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.android.material.color.MaterialColors
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.ExtraColors
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.ui.viewmodels.QuizAttemptViewModel
import com.meegoo.quizproject.server.data.dto.UserAnswerDto
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule

@ExperimentalMaterialApi
@ExperimentalPagerApi
@Composable
fun QuizScreen(
    quizUuid: String,
    viewModel: MainViewModel,
    quizAttemptViewModel: QuizAttemptViewModel,
    navigateToResults: (UUID) -> Unit
) {
    val uuid = UUID.fromString(quizUuid)
    val quiz by remember(uuid) { viewModel.getQuiz(uuid) }

    Scaffold(
        topBar = {
            val navController = AppNavController.current
            TopAppBar(
                title = { Text(quiz?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.launch {
                            quizAttemptViewModel.updateQuizAttempt(viewModel)
                        }
                        navController.popBackStack(Screen.QuizList.route, false)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Menu Btn"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.launch {
                                if (quizAttemptViewModel.updateQuizAttempt(viewModel)) {
                                    if (quizAttemptViewModel.finishAttempt(viewModel)) {
                                        navigateToResults(uuid)
                                    }
                                }
                            }

                        },
                        modifier = Modifier.width(110.dp),
                    ) {
                        if (viewModel.errors.postQuizAttemptError.value == null) {
                            Row(Modifier.padding(5.dp)) {
                                Text(text = "Finish", Modifier.padding(10.dp, 0.dp))
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Finish",
                                    tint = MaterialTheme.extraColors.onPrimaryColored
                                )
                            }

                        } else {
                            IconButton(
                                onClick = {
                                    viewModel.launch {
                                        if (quizAttemptViewModel.updateQuizAttempt(viewModel)) {
                                            if (quizAttemptViewModel.finishAttempt(viewModel)) {
                                                navigateToResults(uuid)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.width(110.dp)
                            ) {
                                Row(Modifier.padding(5.dp)) {
                                    Icon(
                                        imageVector = Icons.Filled.LinkOff,
                                        tint = MaterialTheme.colors.onError,
                                        contentDescription = "Connection Error"
                                    )
                                }
                            }
                        }
                    }

                }
            )

        }
    ) { paddingValues ->

        quizAttemptViewModel.timeRemaining?.let {
            DisposableEffect(key1 = quizUuid) {
                val schedule = Timer().schedule(it * 1000) {
                    viewModel.launch {
                        if (quizAttemptViewModel.updateQuizAttempt(viewModel)) {
                            if (quizAttemptViewModel.finishAttempt(viewModel)) {
                                navigateToResults(uuid)
                            }
                        }
                    }
                }
                onDispose {
                    schedule.cancel()
                }
            }
        }

        QuizContent(quiz!!, viewModel, quizAttemptViewModel, paddingValues)

        if (quizAttemptViewModel.submittingAnswer.value) {
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

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun QuizContent(
    quiz: QuizDto,
    mainViewModel: MainViewModel,
    quizAttemptViewModel: QuizAttemptViewModel,
    paddingValues: PaddingValues
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(paddingValues)
    ) {

        val selectedIndex = rememberPagerState(pageCount = quiz.questionCount!!)

        val focusManager = LocalFocusManager.current
        LaunchedEffect(selectedIndex) {
            snapshotFlow { selectedIndex.currentPage }.collect {
                mainViewModel.launch {
                    quizAttemptViewModel.updateQuizAttempt(mainViewModel)
                    focusManager.clearFocus()
                }
            }
        }

        QuizListScroller(
            quiz.questions, selectedIndex.currentPage,
            onElementChanged = {
                selectedIndex.scrollToPage(it)
            },
            colorMapper = { index, item ->
                if (index == selectedIndex.currentPage) MaterialTheme.colors.secondary
                else if (!quizAttemptViewModel.userAnswers.containsKey(item.id!!)) MaterialTheme.colors.surface
                else {
                    val userAnswer = quizAttemptViewModel.userAnswers[item.id!!]
                    if (userAnswer is UserAnswerDto.ChoiceAnswer && userAnswer.answer.isEmpty() ||
                        userAnswer is UserAnswerDto.StringAnswer && userAnswer.answer.isEmpty()
                    ) {
                        MaterialTheme.colors.surface
                    } else {
                        MaterialTheme.colors.primary
                    }
                }
            }) { index, _ ->
            Box(contentAlignment = Alignment.Center) {
                Text(
                    (index + 1).toString(),
                    textAlign = TextAlign.Center,
                ) // card's content
            }
        }

        val timeFormatted = timeRemaining(quizAttemptViewModel.timeRemaining, quiz.id)

        HorizontalPager(state = selectedIndex) { page ->
            QuestionCard(
                quiz.questions[page],
                selectedIndex,
                page,
                quizAttemptViewModel,
                timeFormatted
            )
        }
    }

}


@ExperimentalPagerApi
@Composable
private fun QuestionCard(
    question: QuestionDto,
    selectedIndex: PagerState,
    page: Int,
    quizAttemptViewModel: QuizAttemptViewModel,
    timeLabel: String
) {
    val scope = rememberCoroutineScope()
    val answer = quizAttemptViewModel.userAnswersOptimized[question.id]
    Column(
        verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 10.dp, 10.dp, 0.dp)
            ) {
                Column(
                    Modifier
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    MarkdownText(
                        text = question.question!!,
                        Modifier.padding(5.dp, 5.dp, 5.dp, 20.dp)
                    )
                    Divider(thickness = 2.dp,  color = MaterialTheme.extraColors.divider)
                    val onValueChange = { it: Any ->
                        quizAttemptViewModel.processUserInput(question, it)
                    }

                    when (question.type) {
                        QuestionDto.QuestionType.SINGLE_ANSWER -> {
                            @Suppress("UNCHECKED_CAST")
                            SingleAnswerCard(question.answers!!, answer as List<Int>, onAnswerChange = onValueChange)
                        }
                        QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                            @Suppress("UNCHECKED_CAST")
                            MultipleAnswerCard(question.answers!!, answer as List<Int>, onAnswerChange = onValueChange)
                        }
                        QuestionDto.QuestionType.TEXT_FIELD -> {
                            TextFieldCard(
                                userAnswer = answer as String,
                                onAnswerChange = onValueChange,
                                isNumeric = false
                            )
                        }
                        QuestionDto.QuestionType.NUMERIC_FIELD -> {
                            TextFieldCard(
                                userAnswer = answer as String,
                                onAnswerChange = {
                                    val newValue = it.replace(",", ".").trim()
                                    if (newValue.isEmpty() || newValue == "-" || newValue.toDoubleOrNull() != null) {
                                        onValueChange(newValue)
                                    }
                                },
                                isNumeric = true
                            )
                        }
                    }
                }
            }
        }


        //Bottom row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 20.dp)
                .height(IntrinsicSize.Max)

        ) {
            if (page > 0) {
                Button(
                    onClick = {
                        scope.launch {
                            selectedIndex.scrollToPage(selectedIndex.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .width(100.dp)
                ) {
                    Text("Previous")
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .width(100.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = timeLabel)
                if (quizAttemptViewModel.updateQuizAttemptError.value) {
                    Text(
                        text = "Connection Error",
                        color = MaterialTheme.colors.onError,
                    )
                }
            }
            if (page < (selectedIndex.pageCount - 1)) {

                Button(
                    onClick = {
                        scope.launch {
                            selectedIndex.scrollToPage(selectedIndex.currentPage + 1)
                        }
                    },
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("Next")
                }
            } else {
                Spacer(modifier = Modifier.width(100.dp))
            }
        }
    }
}