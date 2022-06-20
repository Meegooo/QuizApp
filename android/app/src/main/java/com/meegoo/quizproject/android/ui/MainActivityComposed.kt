package com.meegoo.quizproject.android.ui

import FadeAnimation
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.view.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navigation
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.meegoo.quizproject.android.R
import com.meegoo.quizproject.android.ui.screens.*
import com.meegoo.quizproject.android.ui.viewmodels.*
import com.meegoo.quizproject.android.util.parentViewModel
import kotlinx.coroutines.delay
import java.util.*


val AppNavController = compositionLocalOf<NavController> { error("No navcontroller found!") }

@ExperimentalPagerApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
class MainActivityComposed : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var container: View

    override fun onCreate(savedInstanceState: Bundle?) {
        container = findViewById<ViewGroup>(android.R.id.content).rootView
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            //deprecated in API 30
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        super.onCreate(savedInstanceState)
        viewModel.launch {
            viewModel.loadQuizOverview()
        }
        setContent {
            QuizMaterialTheme {
                MainMenu()
            }
        }
    }

    @Composable
    fun MainMenu() {
        val navController = rememberNavController()
        val items = listOf(
            Screen.QuizList,
            Screen.GroupList,
            Screen.Profile,
        )
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: Screen.QuizList.route
        CompositionLocalProvider(AppNavController provides navController) {
            Scaffold(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsWithImePadding(),
                bottomBar = {
                    BottomNavigation {
                        items.forEach { screen ->
                            BottomNavigationItem(
                                icon = { Icon(screen.icon, stringResource(screen.resourceId)) },
                                label = { Text(stringResource(screen.resourceId)) },
                                selected = currentRoute.startsWith(screen.route),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(0) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true

                                    }
                                }
                            )
                        }
                    }
                }
            ) {
                NavHost(
                    navController,
                    startDestination = Screen.QuizList.route,
                    Modifier.padding(it)
                ) {
                    composable(Screen.QuizList.route) {
                        QuizTab(viewModel,
                            onQuizClick = { uuid ->
                                navController.navigateDelayed("${Screen.QuizList.route}/$uuid")
                            },
                            onQuizEdit = { uuid ->
                                if (uuid == null) {
                                    navController.navigateDelayed("${Screen.QuizList.route}/new/edit")
                                } else {
                                    navController.navigateDelayed("${Screen.QuizList.route}/$uuid/edit")
                                }
                            },
                        onCourseEdit = { uuid ->
                            if (uuid == null) {
                                navController.navigateDelayed("${Screen.QuizList.route}/course/new/edit")
                            } else {
                                navController.navigateDelayed("${Screen.QuizList.route}/course/$uuid/edit")
                            }
                        })
                    }


                    //Quiz Read
                    navigation(
                        "${Screen.GroupList.route}/list",
                        Screen.GroupList.route
                    ) {
                        composable("${Screen.GroupList.route}/list") { stackEntry ->

                            val groupViewModel = stackEntry.parentViewModel<GroupViewModel>(navController)
                            GroupTab(groupViewModel, navigateToGroupEdit = {
                                navController.navigateDelayed("${Screen.GroupList.route}/$it/edit")
                            })
                        }

                        composable(
                            "${Screen.GroupList.route}/{uuid}/edit",
                            arguments = listOf(navArgument("uuid") {
                                type = NavType.StringType
                            })
                        ) { stackEntry ->
                            val uuid = stackEntry.arguments?.getString("uuid")!!
                            val groupViewModel = stackEntry.parentViewModel<GroupViewModel>(navController)

                            FadeAnimation {
                                GroupEditorScreen(groupViewModel, uuid)
                            }
                        }
                    }

                    composable(Screen.Profile.route) {
                        ProfileTab()
                    }

                    //Quiz Read
                    navigation(
                        "${Screen.QuizList.route}/{uuid}/overview",
                        "${Screen.QuizList.route}/{uuid}"
                    ) {
                        //Quiz Overview
                        composable(
                            "${Screen.QuizList.route}/{uuid}/overview",
                            arguments = listOf(navArgument("uuid") {
                                type = NavType.StringType
                            })
                        ) { stackEntry ->
                            val uuid = stackEntry.arguments?.getString("uuid")!!
                            val quizAttemptViewModel = stackEntry.parentViewModel<QuizAttemptViewModel>(navController)
                            quizAttemptViewModel.quiz = viewModel.getQuiz(UUID.fromString(uuid))

                            FadeAnimation {
                                QuizOverviewScreen(
                                    uuid, viewModel, quizAttemptViewModel,
                                    navigateToQuizScreen = { uuid ->
                                        navController.navigateDelayed("${Screen.QuizList.route}/$uuid/active")
                                    },
                                    navigateToResults = { uuid ->
                                        navController.navigateDelayed("${Screen.QuizList.route}/$uuid/results")
                                    })
                            }
                        }

                        //Quiz
                        composable(
                            "${Screen.QuizList.route}/{uuid}/active",
                            arguments = listOf(navArgument("uuid") {
                                type = NavType.StringType
                            })
                        ) { stackEntry ->
                            val uuid = stackEntry.arguments?.getString("uuid")!!
                            val quizAttemptViewModel = stackEntry.parentViewModel<QuizAttemptViewModel>(navController)
                            quizAttemptViewModel.quiz = viewModel.getQuiz(UUID.fromString(uuid))

                            FadeAnimation {
                                QuizScreen(uuid, viewModel,quizAttemptViewModel,
                                    navigateToResults = { uuid ->
                                        navController.navigateDelayed("${Screen.QuizList.route}/$uuid/results")
                                    })
                            }
                        }

                        //Quiz Results
                        composable(
                            "${Screen.QuizList.route}/{uuid}/results",
                            arguments = listOf(navArgument("uuid") {
                                type = NavType.StringType
                            })
                        ) { stackEntry ->
                            val uuid = stackEntry.arguments?.getString("uuid")!!
                            val quizAttemptViewModel = stackEntry.parentViewModel<QuizAttemptViewModel>(navController)
                            quizAttemptViewModel.quiz = viewModel.getQuiz(UUID.fromString(uuid))
                            FadeAnimation {
                                QuizResultsScreen(uuid, viewModel, quizAttemptViewModel)
                            }
                        }
                    }

                    //Quiz Edit
                    navigation(
                        "${Screen.QuizList.route}/{uuid}/editquiz",
                        "${Screen.QuizList.route}/{uuid}/edit"
                    ) {
                        composable(
                            "${Screen.QuizList.route}/{uuid}/editquiz",
                            arguments = listOf(navArgument("uuid") {
                                type = NavType.StringType
                            })
                        ) { stackEntry ->
                            val quizEditViewModel = stackEntry.parentViewModel<QuizEditViewModel>(navController)
                            FadeAnimation {
                                QuizEditorScreen(
                                    viewModel,
                                    quizEditViewModel,
                                    quizUuid = stackEntry.arguments?.getString("uuid")!!,
                                    navigateToQuestionEdit = { uuid ->
                                        navController.navigateDelayed("${Screen.QuizList.route}/editquestions")
                                    },
                                    navigateToAccessControl = { uuid ->
                                        navController.navigateDelayed("${Screen.QuizList.route}/quiz/$uuid/acl")
                                    }
                                )
                            }
                        }

                        composable(
                            "${Screen.QuizList.route}/editquestions"
                        ) { stackEntry ->
                            val quizEditViewModel = stackEntry.parentViewModel<QuizEditViewModel>(navController)
                            FadeAnimation {
                                QuestionListEditorScreen(
                                    viewModel,
                                    quizEditViewModel,
                                    onQuestionEdit = { uuid ->
                                        if (uuid == null) {
                                            navController.navigateDelayed("${Screen.QuizList.route}/editquestion/new")
                                        } else {
                                            navController.navigateDelayed("${Screen.QuizList.route}/editquestion/$uuid")
                                        }
                                    }
                                )
                            }
                        }
                        composable(
                            "${Screen.QuizList.route}/editquestion/{uuid}",
                            arguments = listOf(navArgument("uuid") {
                                type = NavType.StringType
                            })
                        ) { stackEntry ->
                            val quizEditViewModel = stackEntry.parentViewModel<QuizEditViewModel>(navController)
                            val uuid = stackEntry.arguments?.getString("uuid")!!
                            FadeAnimation {
                                QuestionEditorScreen(
                                    quizEditViewModel,
                                    uuid
                                )
                            }
                        }
                    }

                    composable(
                        "${Screen.QuizList.route}/course/{uuid}/edit",
                        arguments = listOf(navArgument("uuid") {
                            type = NavType.StringType
                        })
                    ) { stackEntry ->
                        val courseEditViewModel = viewModel<CourseEditViewModel>()
                        val uuid = stackEntry.arguments?.getString("uuid")!!
                        FadeAnimation {
                            CourseEditorScreen(
                                viewModel,
                                courseEditViewModel,
                                uuid,
                                navigateToAccessControl = {
                                    navController.navigateDelayed("${Screen.QuizList.route}/course/$uuid/acl")
                                }
                            )
                        }
                    }

                    composable("${Screen.QuizList.route}/search/share") { stackEntry ->
                        val ignored =
                            navController.previousBackStackEntry?.savedStateHandle?.get<Array<UUID>>("ignored_quizzes") ?: emptyArray()
                        FadeAnimation {

                            QuizSearchScreen(
                                viewModel,
                                ignored
                            )
                        }
                    }

                    composable(
                        "${Screen.QuizList.route}/course/{uuid}/acl",
                        arguments = listOf(navArgument("uuid") {
                            type = NavType.StringType
                        })
                    ) { stackEntry ->
                        val uuid = stackEntry.arguments?.getString("uuid")!!
                        FadeAnimation {
                            CourseAccessControlScreen(viewModel, UUID.fromString(uuid))
                        }
                    }

                    composable(
                        "${Screen.QuizList.route}/quiz/{uuid}/acl",
                        arguments = listOf(navArgument("uuid") {
                            type = NavType.StringType
                        })
                    ) { stackEntry ->
                        val uuid = stackEntry.arguments?.getString("uuid")!!
                        FadeAnimation {
                            QuizAccessControlScreen(viewModel, UUID.fromString(uuid))
                        }
                    }

                }
            }
        }
    }


    private fun NavController.navigateDelayed(route: String) {
        viewModel.launch {
            delay(50)
            this@navigateDelayed.navigate(route)

        }
    }

}

sealed class Screen(val route: String, @StringRes val resourceId: Int, val icon: ImageVector) {
    object QuizList : Screen("quiz", R.string.menu_quiz, Icons.Filled.Quiz)
//    object CourseList : Screen("course", R.string.menu_courses, Icons.Filled.WorkOutline)
    object GroupList : Screen("group", R.string.menu_groups, Icons.Filled.Groups)
    object Profile : Screen("profile", R.string.menu_profile, Icons.Filled.AccountCircle)
}