package com.meegoo.quizproject.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.ExtraColors
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.ui.viewmodels.QuizEditViewModel
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalMaterialApi
@ExperimentalPagerApi
@Composable
fun QuestionListEditorScreen(
    mainViewModel: MainViewModel,
    quizEditViewModel: QuizEditViewModel,
    onQuestionEdit: (UUID?) -> Unit
) {

    Scaffold(topBar = {
        val navController = AppNavController.current
        TopAppBar(
            title = { Text("${quizEditViewModel.name.value} (Editing)") },
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
            actions = {
                LongPressIconButton(
                    onClick = { QuizApplication.showSortToast("Hold to confirm edit") },
                    onLongClick = {
                        mainViewModel.launch {
                            quizEditViewModel.updateQuiz(mainViewModel)
                        }
                    },
                    enabled = quizEditViewModel.questions.isNotEmpty(),
                ) {
                    Row(Modifier.padding(5.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Confirm edit",
                            tint = if (quizEditViewModel.questions.isNotEmpty()) {
                                MaterialTheme.extraColors.onPrimaryColored
                            } else {
                                MaterialTheme.extraColors.disabledText
                            }
                        )
                    }
                }
            }
        )

    }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(it)
        ) {

            val selectedIndex = rememberPagerState(pageCount = quizEditViewModel.questions.size)

            QuizListScroller(quizEditViewModel.questions, selectedIndex.currentPage,
                onElementChanged = {
                    selectedIndex.scrollToPage(it)
                },
                extrasAfter = {
                    item {
                        Card(
                            onClick = {
                                onQuestionEdit(null)
                            },
                            modifier = Modifier
                                .height(60.dp)
                                .width(60.dp)
                                .padding(5.dp),
                            backgroundColor = MaterialTheme.colors.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", textAlign = TextAlign.Center)
                            }
                        }

                    }
                },
                colorMapper = { index, item ->
                    when (index) {
                        selectedIndex.currentPage -> MaterialTheme.colors.primary
                        else -> MaterialTheme.colors.surface
                    }
                }) { index, _ ->
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        (index + 1).toString(),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (quizEditViewModel.questions.size > 0) {
                HorizontalPager(state = selectedIndex) { page ->
                    QuestionCard(quizEditViewModel.questions[page], selectedIndex, page, onQuestionEdit)
                }
            }
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

@ExperimentalPagerApi
@Composable
private fun QuestionCard(
    question: QuestionDto,
    selectedIndex: PagerState,
    page: Int,
    onQuestionEdit: (UUID?) -> Unit
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
                    when (question.type) {
                        QuestionDto.QuestionType.SINGLE_ANSWER -> {
                            val element = question.answers?.withIndex()?.maxByOrNull {
                                it.value.chosenScore - it.value.notChosenScore
                            }?.index
                            val selected = if (element == null) emptyList() else listOf(element)
                            SingleAnswerCard(question.answers!!, selected)
                        }
                        QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                            val selected = question.answers?.withIndex()?.filter {
                                (it.value.chosenScore - it.value.notChosenScore) > 0
                            }?.map { it.index } ?: emptyList()
                            MultipleAnswerCard(question.answers!!, selected)
                        }

                        QuestionDto.QuestionType.TEXT_FIELD,
                        QuestionDto.QuestionType.NUMERIC_FIELD -> {
                            val systemAnswer = question.answers
                                ?.maxByOrNull {
                                    it.chosenScore - it.notChosenScore
                                }?.text ?: ""
                            TextFieldCard(systemAnswer = systemAnswer, isEnabled = false)
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

            Button(
                onClick = {
                    onQuestionEdit(question.id)
                },
                modifier = Modifier
                    .width(100.dp)
            ) {
                Text("Modify")
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