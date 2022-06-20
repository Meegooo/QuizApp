package com.meegoo.quizproject.android.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.viewmodels.GroupViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.Permissions
import java.util.*
import java.util.function.Function

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalMaterialApi
@Composable
fun GroupTab(
    viewModel: GroupViewModel,
    navigateToGroupEdit: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var editMode by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(Screen.GroupList.resourceId)

    if (!viewModel.groupsLoaded.value) {
        LaunchedEffect(true) {
            viewModel.loadGroups()
        }
    }

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

            AnimatedVisibility(
                editMode,
                enter = slideInHorizontally({ it * 2 }),
                exit = slideOutHorizontally({ it * 2 })
            ) {
                FloatingActionButton(onClick = {
                    navigateToGroupEdit("new")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add group")
                }
            }

        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (editMode) {
                    GroupList(viewModel) {
                        navigateToGroupEdit(it.toString())
                    }
                } else {
                    GroupList(viewModel)
                }

            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun GroupList(
    viewModel: GroupViewModel,
    onGroupEdit: ((uuid: UUID) -> Unit)? = null,
) {

    when {
        //Connection Error
        viewModel.loadingError.value -> {
            ConnectionErrorModule {
                viewModel.viewModelScope.launch {
                    viewModel.loadGroups()
                }
            }
        }

        //Loading
        viewModel.loadingData.value -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.padding(10.dp))
            }
        }

        //Main List
        else -> {
            SwipeRefresh(
                modifier = Modifier.fillMaxHeight(),
                state = rememberSwipeRefreshState(isRefreshing = viewModel.loadingData.value),
                onRefresh = {
                    viewModel.viewModelScope.launch {
                        viewModel.loadGroups()
                    }
                }) {
                LazyColumn(Modifier.fillMaxHeight()) {
                    val groups = if (onGroupEdit == null) {
                        viewModel.groups.values
                    } else {
                        viewModel.groups.values.filter { it.permissions.contains(GrantedPermission.WRITE) }
                    }
                    for (group in groups) {
                        val onGroupEdit = if (onGroupEdit == null) null else {
                            {
                                onGroupEdit(group.id!!)
                            }
                        }
                        stickyHeader {
                            Header(
                                "${group.name ?: ""} (${group.users.size})",
                                onCourseClick = {
                                    viewModel.groupExpanded[group.id] =
                                        !(viewModel.groupExpanded[group.id] ?: false)
                                },
                                onGroupEdit = onGroupEdit,
                                permissions = group.permissions
                            )
                        }

                        val usersSorted = group.users.entries.sortedWith(Comparator
                            .comparing<Map.Entry<String, GrantedPermission>, Int> { it.value.level }
                            .reversed()
                            .thenComparing(Function { it.key })
                        ).map { it.key to it.value }

                        items(usersSorted) { (name, permission) ->
                            AnimatedVisibility(
                                viewModel.groupExpanded[group.id] == true,
                                enter = expandVertically(Alignment.Top),
                                exit = shrinkVertically(Alignment.Top)
                            ) {
                                UserCard(name, permission)
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
    onGroupEdit: (() -> Unit)? = null,
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
            if (onGroupEdit != null) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 20.dp)
                ) {

                    PermissionWindow(permissions
                        .map { if (it == GrantedPermission.OWNER) GrantedPermission.ADMINISTRATION else it },
                        startAt = GrantedPermission.WRITE)
                    IconButton(onClick = onGroupEdit) {
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


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UserCard(
    name: String,
    permission: GrantedPermission
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.height(60.dp)
        ) {
            val color = when (permission) {
                GrantedPermission.OWNER -> MaterialTheme.extraColors.red
                GrantedPermission.WRITE -> MaterialTheme.extraColors.purple
                else -> Color.Unspecified
            }
            Text(name, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.h5, color = color)
        }
        Divider(color = MaterialTheme.extraColors.divider)
    }

}
