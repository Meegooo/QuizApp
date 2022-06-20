package com.meegoo.quizproject.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.meegoo.quizproject.android.R
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalMaterialApi
@Composable
fun QuizTab(
    viewModel: MainViewModel,
    onQuizClick: (uuid: UUID) -> Unit,
    onQuizEdit: (uuid: UUID?) -> Unit,
    onCourseEdit: (uuid: UUID?) -> Unit
) {

    val coroutineScope = rememberCoroutineScope()
    var editMode by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(Screen.QuizList.resourceId)

    Scaffold(
        topBar = {
            TopAppBar(
                {
                    if (editMode) {
                        Text(text = "$title (Editing)")
                    } else {
                        Text(text = title)
                    }
                },
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
                            DropdownMenuItem(onClick = {
                                coroutineScope.launch {
                                    expanded = false
                                    delay(100)
                                    editMode = !editMode
                                }
                            }) {
                                if (!editMode) {
                                    Text("Edit")
                                } else {
                                    Text("Stop edit")
                                }
                            }
                        }
                    }
                }
            )
        },

        floatingActionButton = {
            var fabExpanded by remember { mutableStateOf((false)) }
            AnimatedVisibility(
                editMode && viewModel.errors.loadQuizOverviewEditError.value == null,
                enter = slideInHorizontally({ it * 2 }),
                exit = slideOutHorizontally({ it * 2 })
            ) {
                MultiFloatingActionButton(
                    Icons.Default.Add,
                    items = listOf(
                        MultiFabItem("quiz", R.drawable.baseline_quiz_black_24, "Add Quiz"),
                        MultiFabItem("course", R.drawable.baseline_work_outline_black_24, "Add Quiz Bundle")
                    ),
                    expanded = fabExpanded,
                    onFabItemClicked = {
                        when (it.identifier) {
                            "quiz" -> onQuizEdit(null)
                            "course" -> onCourseEdit(null)
                        }
                    },
                    stateChanged = {
                        fabExpanded = it
                    }
                )
            }
        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (!editMode) {
                    GroupList(viewModel, onQuizClick)
                } else {
                    QuizListEdit(viewModel, onQuizEdit, onCourseEdit)
                    BackHandler {
                        editMode = false
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun GroupList(
    viewModel: MainViewModel,
    onQuizClick: (uuid: UUID) -> Unit
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
            val (quizzesInCourses, quizzesNotInCourses) = remember(viewModel.quizCourseUpdates.value) { viewModel.quizzes() }
            val loadAttempt: suspend (quizUuid: UUID, force: Boolean) -> Unit = remember {
                { quizUuid, force ->
                    viewModel.loadQuizAttempt(quizUuid, force);
                }
            }
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = viewModel.quizListRefreshing.value),
                onRefresh = {
                    viewModel.launch {
                        viewModel.loadQuizOverview(true)
                        viewModel.clearQuizAttempts()
                    }
                }) {
                LazyColumn(Modifier.fillMaxHeight()) {

                    for ((course, quizzes) in quizzesInCourses) {
                        val q = quizzes.filter { it.publishedAt != null }
                        if (q.isNotEmpty()) {
                            stickyHeader {
                                Header("${course.name ?: ""} (${q.size})", onCourseClick = {
                                    viewModel.courseExpanded[course.id] =
                                        !(viewModel.courseExpanded[course.id] ?: false)
                                })
                            }
                            items(q) { quiz ->
                                AnimatedVisibility(
                                    viewModel.courseExpanded[course.id] == true,
                                    enter = expandVertically(Alignment.Top),
                                    exit = shrinkVertically(Alignment.Top)
                                ) {
                                    QuizCard(
                                        onQuizClick,
                                        quiz,
                                        viewModel.getQuizAttempt(quiz.id!!).value,
                                        false,
                                        loadAttempt
                                    )
                                }
                            }
                        }
                    }

                    val q = quizzesNotInCourses.filter { it.publishedAt != null }
                    if (q.isNotEmpty()) {
                        stickyHeader {
                            Header("Others (${q.size})", onCourseClick = {
                                viewModel.courseExpanded[null] = !(viewModel.courseExpanded[null] ?: false)
                            })
                        }

                        items(q) { quiz ->
                            AnimatedVisibility(
                                viewModel.courseExpanded[null] == true,
                                enter = expandVertically(Alignment.Top),
                                exit = shrinkVertically(Alignment.Top)
                            ) {
                                QuizCard(
                                    onQuizClick,
                                    quiz,
                                    viewModel.getQuizAttempt(quiz.id!!).value,
                                    false,
                                    loadAttempt
                                )
                            }
                        }

                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
private fun QuizListEdit(
    viewModel: MainViewModel,
    onQuizEditClick: (uuid: UUID) -> Unit,
    onCourseEditClick: (uuid: UUID) -> Unit
) {
    val (quizzesInCourses, quizzesNotInCourses) = remember(viewModel.quizCourseUpdates.value) { viewModel.quizzesEditable() }
    if (quizzesInCourses.size == 1 && quizzesNotInCourses.isEmpty()) {
        LaunchedEffect(true) {
            viewModel.loadQuizOverview(true)
        }
    }
    when {
        //Connection Error
        viewModel.errors.loadQuizOverviewEditError.value != null -> {
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
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = viewModel.quizListRefreshing.value),
                onRefresh = {
                    viewModel.launch {
                        viewModel.loadQuizOverview(true)
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(Modifier.fillMaxHeight()) {
                    for ((course, quizzes) in quizzesInCourses) {
                        stickyHeader {
                            val onCourseEdit = if (course.permissions.contains(GrantedPermission.WRITE)) {
                                {
                                    onCourseEditClick(course.id!!)
                                }
                            } else null

                            Header(
                                "${course.name ?: ""} (${quizzes.size})",
                                onCourseClick = {
                                    viewModel.courseExpanded[course.id] =
                                        !(viewModel.courseExpanded[course.id] ?: false)
                                },
                                onCourseEdit = onCourseEdit,
                                permissions = course.permissions
                            )
                        }
                        items(quizzes) { quiz ->
                            AnimatedVisibility(
                                viewModel.courseExpanded[course.id] == true,
                                enter = expandVertically(Alignment.Top),
                                exit = shrinkVertically(Alignment.Top)
                            ) {
                                QuizCard(
                                    onQuizEditClick,
                                    quiz,
                                    viewModel.getQuizAttempt(quiz.id!!).value,
                                    true
                                )
                            }
                        }

                    }
                    if (quizzesNotInCourses.isNotEmpty()) {
                        stickyHeader {
                            Header("Others (${quizzesNotInCourses.size})",
                                onCourseClick = {
                                viewModel.courseExpanded[null] = !(viewModel.courseExpanded[null] ?: false)
                            })
                        }
                        items(quizzesNotInCourses) { quiz ->
                            AnimatedVisibility(
                                viewModel.courseExpanded[null] == true,
                                enter = expandVertically(Alignment.Top),
                                exit = shrinkVertically(Alignment.Top)
                            ) {
                                QuizCard(
                                    onQuizEditClick,
                                    quiz,
                                    viewModel.getQuizAttempt(quiz.id!!).value,
                                    true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Header(
    name: String,
    onCourseClick: () -> Unit,
    onCourseEdit: (() -> Unit)? = null,
    permissions: List<GrantedPermission> = emptyList()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .height(60.dp)
            .background(MaterialTheme.colors.surface, shape = RectangleShape),
        elevation = 4.dp,
        onClick = {
            onCourseClick()
        }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.h5)
            if (onCourseEdit != null) {
                Row(horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end=20.dp)) {

                    PermissionWindow(permissions, startAt = GrantedPermission.WRITE)
                    IconButton(onClick = onCourseEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

            }
        }
    }
}
