package com.meegoo.quizproject.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.LoginRepository
import com.meegoo.quizproject.android.ui.Screen
import com.meegoo.quizproject.android.ui.modules.*
import com.meegoo.quizproject.android.ui.viewmodels.MainViewModel
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.meegoo.quizproject.android.ui.extraColors
import java.util.*

@Composable
fun ProfileTab() {
    val title = stringResource(Screen.Profile.resourceId)

    Scaffold(
        topBar = { TopAppBar({ Text(text = title) }) },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                ProfileScreen()
            }
        }
    )
}

@Composable
private fun ProfileScreen(
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Account picture",
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.width(32.dp))
            Text(LoginRepository.username ?: "", style = MaterialTheme.typography.h4)
        }
        Divider(color = MaterialTheme.extraColors.divider)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    LoginRepository.logout(context)
                }
                .padding(horizontal = 10.dp, vertical = 16.dp),
        ) {
            Text("Log out", style = MaterialTheme.typography.h6)
        }
        Divider(color = MaterialTheme.extraColors.divider)

    }
}
