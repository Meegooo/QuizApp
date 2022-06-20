package com.meegoo.quizproject.android.ui.login

import FadeAnimation
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.pager.ExperimentalPagerApi
import com.meegoo.quizproject.android.data.LoginRepository
import com.meegoo.quizproject.android.ui.QuizMaterialTheme
import com.meegoo.quizproject.android.ui.MainActivityComposed
import com.meegoo.quizproject.android.ui.Screen
import kotlinx.coroutines.launch


class LoginActivityComposed : AppCompatActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    @ExperimentalComposeUiApi
    @Composable
    fun LoginFields() {
        val composableScope = rememberCoroutineScope()
        val loading = loginViewModel.loading.value

        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Login") }, Modifier
                    .fillMaxWidth(1f)
            )
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(it)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val usernameFocusRequester = FocusRequester()
                val passwordFocusRequester = FocusRequester()
                AutofillTextField(
                    state = loginViewModel.username,
                    onValueChange = {
                        loginViewModel.username.value = it`
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusOrder(usernameFocusRequester) {
                            passwordFocusRequester.requestFocus()
                        },
                    label = "Username",
                    autofillType = AutofillType.Username,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                )

                Spacer(Modifier.height(16.dp))

                AutofillTextField(
                    state = loginViewModel.password,
                    onValueChange = {
                        loginViewModel.password.value = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusOrder(passwordFocusRequester),
                    label = "Password",
                    autofillType = AutofillType.Password,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    onDone = {
                        composableScope.launch {
                            loginViewModel.login()
                        }
                    }
                )
                Spacer(Modifier.height(32.dp))
                Button(modifier = Modifier
                    .width(200.dp)
                    .height(45.dp),
                    onClick = {
                        composableScope.launch {
                            loginViewModel.login()
                        }
                    }) {
                    Text("Login", style = MaterialTheme.typography.h6)
                }


                Spacer(Modifier.height(40.dp))
                Button(modifier = Modifier
                    .width(200.dp)
                    .height(45.dp),
                    onClick = {
                        loginViewModel.onRegister()
                    }) {
                    Text("Register", style = MaterialTheme.typography.h6)
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(10.dp))
                }
            }
        }
    }

    @ExperimentalComposeUiApi
    @Composable
    fun RegisterFields() {
        val composableScope = rememberCoroutineScope()
        val loading = loginViewModel.loading.value

        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Register") },
                modifier = Modifier.fillMaxWidth(1f),
                navigationIcon = {
                    IconButton(onClick = {
                        loginViewModel.onLogin()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Menu Btn"
                        )
                    }

                }
            )
        }) {

            BackHandler {
                loginViewModel.onLogin()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(it)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                    val usernameErrorMessage = if (loginViewModel.username.value.isBlank()) {
                    "Username may not be empty"
                } else {
                    ""
                }
                val passwordErrorMessage = if (loginViewModel.password.value.isBlank()) {
                    "Password may not be empty"
                } else {
                    ""
                }
                val passwordErrorMessage2 = if (loginViewModel.repeatPassword.value != loginViewModel.password.value) {
                    "Passwords must match"
                } else {
                    ""
                }
                val usernameFocusRequester = FocusRequester()
                val passwordFocusRequester = FocusRequester()
                val passwordFocusRequester2 = FocusRequester()
                AutofillTextField(
                    state = loginViewModel.username,
                    onValueChange = {
                        loginViewModel.username.value = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusOrder(usernameFocusRequester) {
                            passwordFocusRequester.requestFocus()
                        },
                    label = "Username",
                    autofillType = AutofillType.Username,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    error = usernameErrorMessage
                )

                Spacer(Modifier.height(16.dp))

                AutofillTextField(
                    state = loginViewModel.password,
                    onValueChange = {
                        loginViewModel.password.value = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusOrder(passwordFocusRequester) {
                            passwordFocusRequester2.requestFocus()
                        },
                    label = "Password",
                    autofillType = AutofillType.Password,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    error = passwordErrorMessage
                )

                Spacer(Modifier.height(16.dp))
                AutofillTextField(
                    state = loginViewModel.repeatPassword,
                    onValueChange = {
                        loginViewModel.repeatPassword.value = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusOrder(passwordFocusRequester2),
                    label = "Repeat password",
                    autofillType = AutofillType.Password,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    onDone = {
                        composableScope.launch {
                            loginViewModel.login()
                        }
                    },
                    error = passwordErrorMessage2
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    modifier = Modifier
                        .width(250.dp)
                        .height(50.dp),
                    onClick = {
                        composableScope.launch {
                            loginViewModel.register()
                        }
                    },
                    enabled = loginViewModel.repeatPassword.value == loginViewModel.password.value
                            && loginViewModel.password.value.isNotBlank()
                            && loginViewModel.username.value.isNotBlank()
                ) {
                    Text("Register", style = MaterialTheme.typography.h6)
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(10.dp))
                }
            }
        }
    }

    @ExperimentalComposeUiApi
    @Composable
    fun AutofillTextField(
        state: MutableState<String>,
        label: String,
        autofillType: AutofillType,
        onValueChange: (String) -> Unit,
        keyboardOptions: KeyboardOptions,
        modifier: Modifier = Modifier,
        error: String = "",
        onDone: KeyboardActionScope.() -> Unit = {}
    ) {
        val autofillNode = AutofillNode(
            autofillTypes = listOf(autofillType),
            onFill = { state.value = it }
        )
        val autofill = LocalAutofill.current
        LocalAutofillTree.current += autofillNode
        val rememberState by remember { state }
        val passwordTransformation = remember { PasswordVisualTransformation() }


        TextField(
            value = rememberState,
            modifier = modifier
                .onGloballyPositioned {
                    autofillNode.boundingBox = it.boundsInWindow()
                }
                .onFocusChanged { focusState ->
                    autofill?.run {
                        if (focusState.isFocused) {
                            requestAutofillForNode(autofillNode)
                        } else {
                            cancelAutofillForNode(autofillNode)
                        }
                    }
                },
            onValueChange = onValueChange,
            label = { Text(text = label) },
            keyboardOptions = keyboardOptions,
            singleLine = true,
            isError = error.isNotBlank(),
            keyboardActions = KeyboardActions(onDone = onDone),
            visualTransformation =
            if (keyboardOptions.keyboardType == KeyboardType.Password)
                passwordTransformation
            else
                VisualTransformation.None
        )
    }

    @ExperimentalPagerApi
    @ExperimentalMaterialApi
    @ExperimentalComposeUiApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginViewModel.loginResult.observe(this, {
            if (it == null) {
                return@observe
            }
            if (it.error != null) {
                Toast.makeText(this, it.error!!, Toast.LENGTH_SHORT).show()
            }
            if (it.success) {
                startMainActivity()
            }
        })
        lifecycleScope.launch {
            if (LoginRepository.checkLogin()) {
                Log.d("LOGIN", "Starting Main Activity")
                startMainActivity()
            } else {
                setContent {
                    QuizMaterialTheme {
                        if (loginViewModel.registering.value) {
                            FadeAnimation {
                                RegisterFields()
                            }
                        } else {
                            FadeAnimation {
                                LoginFields()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        return
    }

    @ExperimentalMaterialApi
    @ExperimentalPagerApi
    @ExperimentalAnimationApi
    fun startMainActivity() {
        val i = Intent(this, MainActivityComposed::class.java)
        startActivity(i)
        setResult(Activity.RESULT_OK)
        finish()
    }
}