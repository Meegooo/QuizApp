package com.meegoo.quizproject.android.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

@Composable
inline fun <reified VM : ViewModel> NavBackStackEntry.parentViewModel(
    navController: NavController
): VM {
    // First, get the parent of the current destination
    // This always exists since every destination in your graph has a parent
    val parentId = destination.parent!!.id

    // Now get the NavBackStackEntry associated with the parent
    val parentBackStackEntry = navController.getBackStackEntry(parentId)

    // And since we can't use viewModel(), we use ViewModelProvider directly
    // to get the ViewModel instance, using the lifecycle-viewmodel-ktx extension
    return ViewModelProvider(parentBackStackEntry).get(VM::class.java)
}