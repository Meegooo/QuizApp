package com.meegoo.quizproject.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.ui.ExtraColors
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.ui.modules.MultipleAnswerCard
import com.meegoo.quizproject.android.ui.viewmodels.QuizAttemptViewModel
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun QuizResultsScreen(
    quizUuid: String,
    viewModel: MainViewModel,
    quizAttemptViewModel: QuizAttemptViewModel,
) {
    val uuid = UUID.fromString(quizUuid)
    val quiz by remember(quizUuid) { viewModel.getQuiz(uuid) }

    val navController = AppNavController.current
    BackHandler {
        navController.popBackStack(Screen.QuizList.route, false)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quiz?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack(Screen.QuizList.route, false)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Menu Btn"
                        )
                    }
                },
            )
        }
    ) { paddingValues ->

        if (quizAttemptViewModel.systemAnswers.isEmpty()) {
            viewModel.errors.loadQuizAttemptError.value = "Connection Error"
        }
        if (viewModel.errors.loadQuizAttemptError.value != null) {
            ConnectionErrorModule {
                viewModel.launch {
                    val attempt = viewModel.loadQuizAttempt(uuid, true)
                    if (attempt.value != null) {
                        quizAttemptViewModel.fromQuizAttemptDto(attempt.value!!)
                    }
                }
            }
        } else {
            QuizResultsContent(quiz!!, quizAttemptViewModel, paddingValues)
        }

    }
}


@OptIn(ExperimentalPagerApi::class)
@Composable
private fun QuizResultsContent(quiz: QuizDto, quizAttemptViewModel: QuizAttemptViewModel, paddingValues: PaddingValues) {
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(paddingValues)
    ) {
        val selectedIndex = rememberPagerState(pageCount = quiz.questionCount!!)

        val parsed = remember {
            quiz.questions.associateWith { quizAttemptViewModel.parseGrade(it) }
        }
        QuizListScroller(
            quiz.questions, selectedIndex.currentPage,
            onElementChanged = {
                scope.launch { selectedIndex.scrollToPage(it) }
            },
            colorMapper = { index, item ->
                when {
                    index == selectedIndex.currentPage -> MaterialTheme.colors.primary
                    parsed[item]?.isFullMark() ?: false -> MaterialTheme.extraColors.green
                    parsed[item]?.isWrong() ?: false -> MaterialTheme.extraColors.red
                    parsed[item]?.isPartiallyCorrect() ?: false -> MaterialTheme.extraColors.yellow
                    else -> MaterialTheme.colors.surface
                }
            }
        ) { index, item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    (index + 1).toString(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "%.1f/%.1f".format(parsed[item]?.grade ?: 0, parsed[item]?.maxGrade ?: 0),
                    fontSize = 12.sp
                )
            }
        }

        HorizontalPager(state = selectedIndex) { page ->
            val grade = remember(page) { quizAttemptViewModel.parseGrade(quiz.questions[page]) }
            if (grade != null) {
                QuestionCard(quiz.questions[page], selectedIndex, page, grade)
            }
        }
    }
}

@ExperimentalPagerApi
@Composable
private fun QuestionCard(
    question: QuestionDto,
    selectedIndex: PagerState,
    page: Int,
    grade: QuizAttemptDto.Grade
) {
    val scope = rememberCoroutineScope()
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
                    Divider(thickness = 2.dp, color = MaterialTheme.extraColors.divider)

                    val colorMapper: @Composable (element: QuestionDto.AnswerDto) -> Color = {
                        grade as QuizAttemptDto.Grade.ChoiceGrade
                        when (it.id) {
                            in grade.correctAnswers -> {
                                MaterialTheme.extraColors.greenTranslucent
                            }
                            in grade.wrongAnswers -> {
                                MaterialTheme.extraColors.redTranslucent
                            }
                            else -> {
                                Color.Transparent
                            }
                        }
                    }

                    when (question.type) {
                        QuestionDto.QuestionType.SINGLE_ANSWER -> {
                            grade as QuizAttemptDto.Grade.ChoiceGrade
                            val userAnswers = grade.userAnswer?.answer ?: emptyList()
                            val selected = question.answers?.withIndex()?.filter {
                                it.value.id in userAnswers
                            }?.map { it.index } ?: emptyList()
                            SingleAnswerCard(question.answers!!, selectedIndex = selected, colorMapper = colorMapper)

                        }
                        QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                            grade as QuizAttemptDto.Grade.ChoiceGrade
                            val userAnswers = grade.userAnswer?.answer ?: emptyList()
                            val selected = question.answers?.withIndex()?.filter {
                                it.value.id in userAnswers
                            }?.map { it.index } ?: emptyList()
                            MultipleAnswerCard(question.answers!!, selectedIndex = selected, colorMapper = colorMapper)
                        }

                        QuestionDto.QuestionType.TEXT_FIELD,
                        QuestionDto.QuestionType.NUMERIC_FIELD -> {
                            grade as QuizAttemptDto.Grade.StringGrade
                            val color = when {
                                grade.isFullMark() -> {
                                    MaterialTheme.extraColors.greenTranslucent
                                }
                                grade.isWrong() -> {
                                    MaterialTheme.extraColors.redTranslucent
                                }
                                grade.isPartiallyCorrect() -> {
                                    MaterialTheme.extraColors.yellowTranslucent
                                }
                                else -> {
                                    Color.Transparent
                                }
                            }

                            TextFieldCard(
                                userAnswer = grade.userAnswer?.answer ?: "",
                                systemAnswer = grade.systemAnswer.answer,
                                isEnabled = false,
                                userAnswerColor = color
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