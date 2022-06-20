package com.meegoo.quizproject.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.meegoo.quizproject.android.R
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.modules.ConnectionErrorModule
import com.meegoo.quizproject.android.ui.modules.timeRemaining
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.ui.viewmodels.QuizAttemptViewModel
import com.meegoo.quizproject.android.util.format
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun QuizOverviewScreen(
    quizUuid: String,
    viewModel: MainViewModel,
    quizAttemptViewModel: QuizAttemptViewModel,
    navigateToQuizScreen: (uuid: UUID) -> Unit,
    navigateToResults: (UUID) -> Unit
) {
    val uuid = UUID.fromString(quizUuid)
    LaunchedEffect(quizUuid) {
        viewModel.loadQuiz(uuid)
        viewModel.loadQuizAttempt(uuid)
    }
    val quiz = viewModel.getQuiz(uuid)
    val quizAttempt = viewModel.getQuizAttempt(uuid)

    Scaffold(
        topBar = {
            val navController = AppNavController.current
            TopAppBar(
                title = { Text(quiz.value?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back button"
                        )
                    }
                },
            )

        }) {

        when {
            //Loading quiz attempt error
            viewModel.errors.loadQuizAttemptError.value != null -> {
                ConnectionErrorModule {
                    viewModel.launch {
                        viewModel.loadQuizAttempt(uuid, true)
                    }
                }
            }

            //Loading quiz error
            viewModel.errors.loadQuizError.value != null -> {
                ConnectionErrorModule {
                    viewModel.launch {
                        viewModel.loadQuiz(uuid, true)
                    }
                }
            }

            //Loading quiz error
            viewModel.errors.createQuizAttemptError.value != null -> {
                ConnectionErrorModule {
                    viewModel.launch {
                        viewModel.createQuizAttempt(uuid, true)
                    }
                }
            }

            //Quiz loaded
            quiz.value != null && quizAttempt.value != null && !viewModel.loadingQuiz.value -> {

                LaunchedEffect(quizAttempt.value) {
                    quizAttemptViewModel.fromQuizAttemptDto(quizAttempt.value!!)
                    quizAttemptViewModel.loadUserAnswers()
                }

                val refreshState =
                    rememberSwipeRefreshState(isRefreshing = viewModel.quizOverviewRefreshing.value)
                SwipeRefresh(state = refreshState, onRefresh = {
                    viewModel.launch {
                        viewModel.loadQuiz(quiz.value?.id!!, true)
                        viewModel.loadQuizAttempt(quiz.value?.id!!, true)
                        quizAttemptViewModel.fromQuizAttemptDto(quizAttempt.value!!)
                    }
                }) {
                    QuizOverviewContent(
                        quiz = quiz.value!!,
                        quizAttemptViewModel = quizAttemptViewModel,
                        viewModel = viewModel,
                        paddingValues = it,
                        onStartAttempt = { uuid ->
                            viewModel.launch {
                                val attempt = viewModel.createQuizAttempt(uuid)
                                attempt.value?.let {
                                    quizAttemptViewModel.fromQuizAttemptDto(it)
                                    navigateToQuizScreen(uuid)
                                }
                            }
                        },
                        onShowResults = navigateToResults
                    )
                }
            }
            else -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}


@Composable
private fun QuizOverviewContent(
    quiz: QuizDto,
    quizAttemptViewModel: QuizAttemptViewModel,
    viewModel: MainViewModel,
    paddingValues: PaddingValues,
    onStartAttempt: (uuid: UUID) -> Unit,
    onShowResults: (UUID) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
    ) {
        Text(
            "${stringResource(R.string.created_at)}: ${quiz.publishedAt.format()}",
            Modifier.padding(10.dp)
        )
        Text(
            "${stringResource(R.string.questions)}: ${quiz.questions.size ?: 0}",
            Modifier.padding(10.dp)
        )

        Divider(color = MaterialTheme.extraColors.divider)
        if (quiz.questions.size > 0 && quiz.publishedAt != null) {
            if (quizAttemptViewModel.newAttempt.value) {
                Text(
                    "${stringResource(R.string.time_limit)}: ${quiz.timeLimitFormatted}",
                    Modifier.padding(10.dp)
                )

                Spacer(Modifier.height(20.dp))
                Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        modifier = Modifier.width(200.dp),
                        onClick = {
                            onStartAttempt(quiz.id!!)
                        }
                    ) {
                        Text("Begin")
                    }
                }
            } else if (quizAttemptViewModel.isExpired() == false) {

                val timeFormatted = timeRemaining(quizAttemptViewModel.timeRemaining, quiz.id, onFinish = {
                    viewModel.launch {
                        quizAttemptViewModel.finishAttempt(viewModel)
                    }
                })

                Text(
                    "${stringResource(R.string.time_remaining)}: $timeFormatted",
                    Modifier.padding(10.dp)
                )

                Spacer(Modifier.height(20.dp))
                Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        modifier = Modifier.width(200.dp),
                        onClick = { onStartAttempt(quiz.id!!) }
                    ) {
                        Text("Continue")
                    }
                }
            } else if (quizAttemptViewModel.systemAnswers.isNotEmpty()) {
                Text(
                    "${stringResource(R.string.finished_at)}: ${quizAttemptViewModel.finishedAt.format()}",
                    Modifier.padding(10.dp)
                )
                Text(
                    "${stringResource(R.string.time_taken)}: ${quizAttemptViewModel.timeTakenFormatted}",
                    Modifier.padding(10.dp)
                )
                Text(
                    "${stringResource(R.string.score)}: ${
                        String.format(
                            "%.1f/%.1f",
                            quizAttemptViewModel.score.value ?: 0,
                            quiz.score
                        )
                    }",
                    Modifier.padding(10.dp)
                )

                Spacer(Modifier.height(20.dp))
                Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        modifier = Modifier.width(200.dp),
                        onClick = { onShowResults(quiz.id!!) }
                    ) {
                        Text("Show Results")
                    }
                }
            } else {
                LaunchedEffect(quiz.id!!) {
                    delay(1000)
                    if (quizAttemptViewModel.systemAnswers.isEmpty()) {
                        viewModel.loadQuizAttempt(quiz.id!!, true)
                    }
                }
            }
        } else {
            Text(
                "Quiz doesn't contain question or is not published",
                Modifier.padding(10.dp)
            )
        }
    }

}