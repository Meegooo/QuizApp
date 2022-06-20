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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.ui.AppNavController
import com.meegoo.quizproject.android.ui.extraColors
import com.meegoo.quizproject.android.ui.modules.LongPressButton
import com.meegoo.quizproject.android.ui.viewmodels.CourseEditViewModel
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.meegoo.quizproject.android.ui.modules.CheckboxWithText
import com.meegoo.quizproject.android.ui.modules.LongPressIconButton
import com.meegoo.quizproject.android.ui.modules.namePrompt
import com.meegoo.quizproject.android.ui.viewmodels.GroupEditViewModel
import com.meegoo.quizproject.android.ui.viewmodels.GroupViewModel
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch
import java.util.*


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GroupEditorScreen(
    groupViewModel: GroupViewModel,
    groupUuid: String,
) {
    val newGroup = groupUuid == "new"
    val group = viewModel<GroupEditViewModel>()
    LaunchedEffect(groupUuid) {
        group.fromGroupViewModel(groupUuid, groupViewModel)
    }
    val navController = AppNavController.current

    val usersSorted = remember(group.updates.value) { group.getUsersSorted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (newGroup) "New Group" else group.name.value) },
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

        val deleteDialog = remember { MaterialDialog() }

        deleteDialog.build {
            title(text = "Confirm deletion")
            message(text = "Please enter group name to confirm deletion")
            input(
                label = "Group Name",
                hint = group.name.value,
                waitForPositiveButton = false
            ) { inputString ->
                if (inputString == group.name.value) {
                    enablePositiveButton()
                } else {
                    disablePositiveButton()
                }
            }
            buttons {
                positiveButton("Delete", onClick = {
                    groupViewModel.viewModelScope.launch {
                        if (groupViewModel.deleteGroup(UUID.fromString(groupUuid))) {
                            navController.popBackStack()
                        }
                    }

                })
                negativeButton("Cancel")
            }
        }


        val userAddDialog = namePrompt("user name") { value ->
            if (usersSorted.none { it.first == value }) {
                group.addUser(value)
            } else {
                "User already in group"
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
            TextField(value = group.name.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 0.dp),
                label = { Text("Name") },

                onValueChange = { newValue ->
                    group.name.value = newValue
                })

            Spacer(Modifier.height(20.dp))
            Divider(color = MaterialTheme.extraColors.divider)


            UserCards(usersSorted,
                onUserAdd = {
                    userAddDialog.show()
                },
                onUserDelete = {
                    group.removeUser(it)
                },
                onPermissionChange = {name, hasWrite ->
                    group.permissionChange(name, hasWrite)
                },
                activeUserPermission = group.permissions.maxByOrNull { it.level } ?: GrantedPermission.NONE
            )

            Spacer(Modifier.height(20.dp))
            Divider(color = MaterialTheme.extraColors.divider)

            //Confirmation buttons
            ConfirmationButtons(
                newGroup,
                groupViewModel,
                group,
                deleteDialog
            )

        }

        if (group.updating.value) {
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

@Composable
private fun ConfirmationButtons(
    newGroup: Boolean,
    groupViewModel: GroupViewModel,
    groupEditViewModel: GroupEditViewModel,
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

        if (newGroup) {
            Button(
                onClick = {
                    groupViewModel.viewModelScope.launch {
                        if (groupEditViewModel.createGroup(groupViewModel)) {
                            navController.popBackStack()
                        }
                    }
                },
                enabled = groupEditViewModel.name.value.isNotBlank(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Create")
            }
        } else {
            Button(
                onClick = {
                    groupViewModel.viewModelScope.launch {
                        if (groupEditViewModel.updateGroup(groupViewModel)) {
                            navController.popBackStack()
                        }
                    }
                },
                enabled = groupEditViewModel.name.value.isNotBlank(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Confirm")
            }
        }
    }


    if (!newGroup) {
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
                    groupEditViewModel.viewModelScope.launch {
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
private fun UserCards(
    users: List<Pair<String, GrantedPermission>>,
    activeUserPermission: GrantedPermission,
    onUserAdd: () -> Unit = {},
    onUserDelete: (String) -> Unit = {},
    onPermissionChange: (String, Boolean) -> Unit = {_, _, ->},
) {
    Column(Modifier.fillMaxWidth()) {
        users.forEachIndexed { idx, element ->
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

                        UserCard(element.first, element.second, activeUserPermission,
                            onUserDelete = {
                                onUserDelete(element.first)
                            },
                            onPermissionChange = {
                                onPermissionChange(element.first, it)
                            }
                        )


                    }

                }
            }

        }
        IconButton(onClick = onUserAdd, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(
                imageVector = Icons.Filled.Add,
                tint = MaterialTheme.colors.onBackground,
                contentDescription = "Add User"
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UserCard(
    name: String,
    permission: GrantedPermission,
    activeUserPermission: GrantedPermission,
    onUserDelete: () -> Unit,
    onPermissionChange: (Boolean) -> Unit,

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (permission.level < activeUserPermission.level) {
                    CheckboxWithText(
                        text = "Write",
                        selected = permission == GrantedPermission.WRITE,
                        onClick = onPermissionChange
                    )
                    LongPressIconButton(
                        onClick = { QuizApplication.showSortToast("Hold to delete") },
                        onLongClick = {
                            onUserDelete()
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
        Divider(color = MaterialTheme.extraColors.divider)
    }

}
