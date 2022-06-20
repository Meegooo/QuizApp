package com.meegoo.quizproject.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.R
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.ui.viewmodels.AclViewModel
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.data.dto.AclDto
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.extraColors
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.buttons
import com.vanpra.composematerialdialogs.input
import com.vanpra.composematerialdialogs.title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


@OptIn(ExperimentalAnimationApi::class)
@ExperimentalMaterialApi
@Composable
fun QuizAccessControlScreen(
    viewModel: MainViewModel,
    uuid: UUID
) {
    val quiz = viewModel.getQuiz(uuid)
    val aclViewModel = viewModel<AclViewModel>()
    LaunchedEffect(uuid) {
        aclViewModel.loadFromQuizId(uuid)
        aclViewModel.updates.value++
    }
    aclViewModel.isOwner.value = (quiz.value?.permissions ?: emptyList()).contains(GrantedPermission.OWNER)
    AccessControlScreen(aclViewModel, quiz.value?.name ?: "")
}

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalMaterialApi
@Composable
fun CourseAccessControlScreen(
    viewModel: MainViewModel,
    uuid: UUID
) {
    val course = viewModel.getCourse(uuid)
    val aclViewModel = viewModel<AclViewModel>()
    LaunchedEffect(uuid) {
        aclViewModel.loadFromCourseId(uuid)
        aclViewModel.updates.value++
    }
    aclViewModel.isOwner.value = (course.value?.permissions ?: emptyList()).contains(GrantedPermission.OWNER)
    AccessControlScreen(aclViewModel, course.value?.name ?: "")
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun AccessControlScreen(
    aclViewModel: AclViewModel,
    title: String
) {

    val groupDialog = namePrompt("group name") { value ->
        if (aclViewModel.accessors.none { it.sid == "group:$value" }) {
            aclViewModel.createGroup(value)
        } else {
            "Group already in list"
        }
    }
    val userDialog = namePrompt("user name") { value ->
        if (aclViewModel.accessors.none { it.sid == "user:$value" }) {
            aclViewModel.createUser(value)
        } else {
            "User already in list"
        }
    }
    val navController = AppNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                {
                    Text(text = "$title (ACL)")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Menu Btn"
                        )
                    }
                },
            )
        },

        floatingActionButton = {
            var fabExpanded by remember { mutableStateOf((false)) }
            MultiFloatingActionButton(
                Icons.Default.Add,
                items = listOf(
                    MultiFabItem("group", R.drawable.outline_group_add_black_24, "Add Group"),
                    MultiFabItem("user", R.drawable.outline_person_add_black_24, "Add User")
                ),
                expanded = fabExpanded,
                onFabItemClicked = {
                    when (it.identifier) {
                        "group" -> groupDialog.show()
                        "user" -> userDialog.show()
                    }
                },
                stateChanged = {
                    fabExpanded = it
                }
            )

        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                GroupList(aclViewModel)
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun GroupList(
    viewModel: AclViewModel
) {

    if (//Connection Error
        viewModel.loadingError.value) {
        ConnectionErrorModule {
            viewModel.reload()
        }
    }

    //Main List
    else {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = viewModel.loadingData.value),
            onRefresh = {
                viewModel.reload()
            }) {

            val groups = remember(viewModel.updates.value) {
                viewModel.accessors
                    .filter { it.sid.startsWith("group:") }
                    .map { AclDto(it.sid.replace("group:", ""), it.permissions) }
            }

            val users = remember(viewModel.updates.value) {
                viewModel.accessors
                    .filter { it.sid.startsWith("user:") }
                    .map { AclDto(it.sid.replace("user:", ""), it.permissions) }
            }

            LazyColumn(Modifier.fillMaxHeight()) {
                AclList(
                    "Groups",
                    groups,
                    showShare = viewModel.aclType == AclViewModel.AclType.QUIZ,
                    isOwner = viewModel.isOwner.value,
                    expanded = viewModel.groupsExpanded.value,
                    onHeaderClick = { viewModel.groupsExpanded.value = !viewModel.groupsExpanded.value },
                    onEntryDelete = {
                        viewModel.deleteGroup(it)
                    },
                    onEntryChange = { group, newPermission ->
                        viewModel.modifyGroup(group, newPermission)
                    }
                )
                AclList(
                    "Users",
                    users,
                    showShare = viewModel.aclType == AclViewModel.AclType.QUIZ,
                    isOwner = viewModel.isOwner.value,
                    expanded = viewModel.usersExpanded.value,
                    onHeaderClick = { viewModel.usersExpanded.value = !viewModel.usersExpanded.value },
                    onEntryDelete = {
                        viewModel.deleteUser(it)
                    },
                    onEntryChange = { user, newPermission ->
                        viewModel.modifyUser(user, newPermission)
                    }
                )
            }
        }

        //Loading
        if (viewModel.loadingData.value) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(MaterialTheme.extraColors.backgroundOverlay)) {
                CircularProgressIndicator(modifier = Modifier.padding(10.dp))
            }
        }
    }
}

@Suppress("FunctionName")
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
private fun LazyListScope.AclList(
    header: String,
    list: List<AclDto>,
    showShare: Boolean,
    isOwner: Boolean,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    onEntryDelete: (sid: String) -> Unit,
    onEntryChange: (sid: String, GrantedPermission) -> Unit
) {
    if (list.isNotEmpty()) {
        stickyHeader {
            Header(header, onHeaderClick = onHeaderClick)
        }
        items(list) { item ->
            AnimatedVisibility(
                expanded,
                enter = expandVertically(Alignment.Top),
                exit = shrinkVertically(Alignment.Top)
            ) {
                val permissions = ArrayList<GrantedPermission>().apply {
                    add(GrantedPermission.READ)
                    if (showShare) add(GrantedPermission.SHARE)
                    add(GrantedPermission.WRITE)
                    if (isOwner) add(GrantedPermission.ADMINISTRATION)
                }
                val selectedPermission = item.permissions.maxByOrNull { it.level } ?: GrantedPermission.NONE
                if (selectedPermission != GrantedPermission.NONE) {
                    AclEntryCard(item.sid, permissions, selectedPermission, isOwner,
                        onEntryDelete = {
                            onEntryDelete(item.sid)
                        },
                        onEntryChange = {
                            onEntryChange(item.sid, it)
                        })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Header(name: String, onHeaderClick: () -> Unit) {
    Divider(
        Modifier
            .height(2.dp)
            .background(MaterialTheme.extraColors.divider)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colors.surface, shape = RectangleShape),
        elevation = 4.dp,
        onClick = {
            onHeaderClick()
        }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, Modifier.padding(16.dp), style = MaterialTheme.typography.h5)
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AclEntryCard(
    name: String,
    permissions: List<GrantedPermission>,
    selectedPermission: GrantedPermission,
    isViewedByOwner: Boolean,
    onEntryDelete: () -> Unit,
    onEntryChange: (GrantedPermission) -> Unit
) {
    Divider(thickness = 2.dp, color = MaterialTheme.extraColors.divider)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface, shape = RectangleShape),
        elevation = 1.dp
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(name, Modifier.padding(16.dp), style = MaterialTheme.typography.h6)
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()) {
                var selected by remember { mutableStateOf(0) }
                val isAdmin = selectedPermission == GrantedPermission.ADMINISTRATION
                val isOwner = selectedPermission == GrantedPermission.OWNER
                val options = if (isOwner) {
                    selected = 0
                    listOf(GrantedPermission.OWNER.label)
                } else if (isAdmin && !isViewedByOwner) {
                    selected = 0
                    listOf(GrantedPermission.ADMINISTRATION.label)
                } else {
                    selected = permissions.indexOf(selectedPermission)
                    permissions.map { it.label }
                }
                val disabled = isOwner || (isAdmin && !isViewedByOwner)
                ExposedDropdownMenu(
                    options = options,
                    selected = selected,
                    label = "Permission",
                    enabled = !disabled,
                    modifier = Modifier
                        .width(200.dp)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    selected = it
                    onEntryChange(permissions[it])
                }
                if (!disabled) {
                    LongPressButton(
                        onClick = { QuizApplication.showSortToast("Hold to delete") },
                        onLongClick = {
                            QuizApplication.vibrate(50)
                            onEntryDelete()
                        },
                        modifier = Modifier.width(150.dp).padding(end = 16.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                }
            }

        }
    }
}
