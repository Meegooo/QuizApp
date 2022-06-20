package com.meegoo.quizproject.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.dto.CourseDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.modules.ConnectionErrorModule
import com.meegoo.quizproject.android.ui.modules.LongPressButton
import com.meegoo.quizproject.android.ui.modules.LongPressIconButton
import com.meegoo.quizproject.android.ui.viewmodels.CourseEditViewModel
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.vanpra.composematerialdialogs.*
import java.util.*
import kotlin.collections.ArrayList


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CourseEditorScreen(
    mainViewModel: MainViewModel,
    courseEditViewModel: CourseEditViewModel,
    courseUuid: String,
    navigateToAccessControl: (UUID) -> Unit
) {
    val newCourse = courseUuid == "new"
    val course: MutableState<CourseDto?> = remember {
        if (newCourse) {
            mutableStateOf(CourseDto())
        } else {
            val uuid = UUID.fromString(courseUuid)
            mainViewModel.loadCourse(uuid)
        }
    }

    val navController = AppNavController.current
    val selectionResult =
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<UUID>("selected_quiz")?.observeAsState()
    selectionResult?.value?.let {
        if (courseEditViewModel.awaitResult) {
            courseEditViewModel.awaitResult = false
            courseEditViewModel.addQuiz(mainViewModel, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course.value?.name ?: "New Bundle") },
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
        //Loading course error
        if (!newCourse && mainViewModel.errors.loadQuizError.value != null) {
            ConnectionErrorModule {
                mainViewModel.loadQuizEditable(UUID.fromString(courseUuid), true)
            }
        } else if (course.value == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.padding(10.dp))
            }
        } else {
            LaunchedEffect(course.value) {
                courseEditViewModel.fromCourseDto(course.value!!, mainViewModel)
            }
            val deleteDialog = remember { MaterialDialog() }

            deleteDialog.build {
                title(text = "Confirm deletion")
                message(text = "Please enter course name to confirm deletion")
                input(
                    label = "Course Name",
                    hint = courseEditViewModel.name.value,
                    waitForPositiveButton = false
                ) { inputString ->
                    if (inputString == courseEditViewModel.name.value) {
                        enablePositiveButton()
                    } else {
                        disablePositiveButton()
                    }
                }
                buttons {
                    positiveButton("Delete", onClick = {
                        mainViewModel.launch {
                            courseEditViewModel.deleteCourse(mainViewModel)
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
                TextField(value = courseEditViewModel.name.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp),
                    label = { Text("Name") },

                    onValueChange = { newValue ->
                        courseEditViewModel.name.value = newValue
                    })

                Spacer(Modifier.height(20.dp))
                Divider(color = MaterialTheme.extraColors.divider)

                val l = ArrayList(courseEditViewModel.quizzes).apply {
                    removeAll { it.id in courseEditViewModel.removedQuizzes }
                    addAll(courseEditViewModel.addedQuizzes)
                }.sortedBy { it.name }
                QuizCards(l,
                    onQuizAdd = {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "ignored_quizzes",
                            l.map { it.id }.toTypedArray()
                        )
                        courseEditViewModel.awaitResult = true
                        navController.navigate("${ Screen.QuizList.route}/search/share")
                    },
                    onQuizDelete = {
                        courseEditViewModel.removeQuiz(it.id!!)
                    })

                Spacer(Modifier.height(20.dp))
                Divider(color = MaterialTheme.extraColors.divider)

                //Confirmation buttons
                ConfirmationButtons(
                    newCourse,
                    mainViewModel,
                    courseEditViewModel,
                    navigateToAccessControl,
                    deleteDialog
                )

            }

            if (courseEditViewModel.updatingCourse.value) {
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
    newCourse: Boolean,
    mainViewModel: MainViewModel,
    courseEditViewModel: CourseEditViewModel,
    navigateToAccessControl: (UUID) -> Unit,
    deleteDialog: MaterialDialog
) {
    Row(
        Modifier
            .padding(10.dp)
            .padding(top = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val navController = AppNavController.current

        if (newCourse) {
            Button(
                onClick = {
                    mainViewModel.launch {
                        courseEditViewModel.createCourse(mainViewModel)
                        if (!courseEditViewModel.updatingCourseError.value) {
                            navController.popBackStack()
                        }
                    }
                },
                enabled = courseEditViewModel.name.value.isNotBlank(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Create")
            }
        } else {
            Button(
                onClick = {
                    mainViewModel.launch {
                        courseEditViewModel.updateCourse(mainViewModel)
                        if (!courseEditViewModel.updatingCourseError.value) {
                            navController.popBackStack()
                        }
                    }
                },
                enabled = courseEditViewModel.name.value.isNotBlank(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Confirm")
            }
        }
    }


    if (courseEditViewModel.permissions.contains(GrantedPermission.ADMINISTRATION)) {
        Row(
            Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                    navigateToAccessControl(courseEditViewModel.id.value!!)
                },
                modifier = Modifier.width(150.dp)
            ) {
                Text("Access Control")
            }
        }
    }


    if (!newCourse) {
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


@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun QuizCards(
    quizzes: List<QuizDto>,
    onQuizAdd: () -> Unit = {},
    onQuizDelete: (QuizDto) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        quizzes.forEachIndexed { idx, element ->
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

                        QuizCard(element, onQuizDelete = {
                            onQuizDelete(element)
                        })


                    }

                }
            }

        }
        IconButton(onClick = onQuizAdd, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(
                imageVector = Icons.Filled.Add,
                tint = MaterialTheme.colors.onBackground,
                contentDescription = "Add Quiz"
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun QuizCard(
    quiz: QuizDto,
    onQuizDelete: () -> Unit
) {
    Card(
        elevation = 2.dp,
        modifier = Modifier
            .padding(PaddingValues(10.dp, 10.dp))
            .fillMaxWidth(1.0f)
            .height(80.dp)
            .background(MaterialTheme.colors.surface),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.padding(16.dp, 8.dp)) {
                Text(
                    quiz.name!!,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.weight(1f)
                )

                if (quiz.publishedAt != null) {
                    Row {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Quiz published",
                            tint = MaterialTheme.extraColors.green,
                            modifier = Modifier.size(20.dp)
                        )

                        Text("${quiz.questionCount ?: 0} Questions", Modifier.padding(start = 5.dp))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = "Quiz not published",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            LongPressIconButton(
                onClick = { QuizApplication.showSortToast("Hold to delete") },
                onLongClick = {
                    onQuizDelete()
                    QuizApplication.vibrate(50)
                }) {
                Icon(
                    imageVector = Icons.Filled.RemoveCircle,
                    tint = MaterialTheme.colors.error,
                    contentDescription = "Remove Answer"
                )
            }
        }
    }
}
