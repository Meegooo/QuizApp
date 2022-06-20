package com.meegoo.quizproject.android.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.modules.ConnectionErrorModule
import com.meegoo.quizproject.android.ui.modules.QuizCard
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalMaterialApi
@Composable
fun QuizSearchScreen(
    viewModel: MainViewModel,
    ignoreQuizzes: Array<UUID> = emptyArray(),
) {
    val navController = AppNavController.current
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Select Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Menu Btn"
                        )
                    }
                })
        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                GroupList(viewModel, onQuizClick = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_quiz", it)
                    navController.popBackStack()
                }, ignoreQuizzes.toSet())
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun GroupList(
    viewModel: MainViewModel,
    onQuizClick: (uuid: UUID) -> Unit,
    ignoreQuizzes: Set<UUID>,
) {

    when {
        //Connection Error
        viewModel.errors.loadQuizOverviewError.value != null -> {
            ConnectionErrorModule {
                viewModel.launch {
                    viewModel.loadQuizOverview(true)
                }
            }
        }

        //Loading
        viewModel.quizListRefreshing.value -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.padding(10.dp))
            }
        }

        //Main List
        else -> {
            var query by rememberSaveable { mutableStateOf("") }
            val quizzesNotFiltered = remember(viewModel.quizCourseUpdates.value, ignoreQuizzes) {
                viewModel.quizzesShareable()
                    .filter { it.id !in ignoreQuizzes }
                    .sortedBy { it.name }

            }
            val quizzes = remember(query) {
                quizzesNotFiltered.filter { query.isBlank() || it.name?.lowercase()?.contains(query.lowercase()) == true }
            }
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = viewModel.quizListRefreshing.value),
                onRefresh = {
                    viewModel.launch {
                        viewModel.loadQuizOverview(true)
                    }
                }) {
                LazyColumn(Modifier.fillMaxHeight().animateContentSize()) {
                    stickyHeader {

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.surface, shape = RectangleShape),
                            elevation = 4.dp) {
                            TextField(
                                value = query,
                                onValueChange = { newValue ->
                                    query = newValue
                                },
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxSize(),
                                label = { Text("Search") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Search, contentDescription = "Quiz Search")
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                )
                            )

                        }

                    }

                    items(quizzes) { quiz ->
                        QuizCard(
                            onQuizClick,
                            quiz
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@Composable
private fun QuizCard(
    onQuizClick: (uuid: UUID) -> Unit,
    quiz: QuizDto,
) {
    QuizCard(
        onQuizClick = onQuizClick,
        quiz = quiz,
        quizAttempt = null,
        editing = true,
        permissionStartsAt = GrantedPermission.SHARE
    )
}