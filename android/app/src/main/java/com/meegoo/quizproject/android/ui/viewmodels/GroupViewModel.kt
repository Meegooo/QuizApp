package com.meegoo.quizproject.android.ui.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.Repository
import com.meegoo.quizproject.server.data.dto.GroupDto
import java.util.*


class GroupViewModel : ViewModel() {
    var groups = mutableStateMapOf<UUID, GroupDto>()

    val groupExpanded = mutableStateMapOf<UUID?, Boolean>()

    var loadingData = mutableStateOf(false)
    var loadingError = mutableStateOf(false)

    var updates = mutableStateOf(0)

    var groupsLoaded = mutableStateOf(false)
    suspend fun loadGroups() {
        loadingData.value = true
        loadingError.value = false
        val response = Repository.loadGroups()
        if (response.isSuccessful()) {
            val list = response.body
            groups.clear()
            groups.putAll(list!!)
            groupsLoaded.value = true
        } else {
            loadingError.value = true
        }
        loadingData.value = false
        updates.value++

    }

    suspend fun createGroup(name: String): Pair<String?, UUID?> {
        val response = Repository.createGroup(name)
        return if (response.isSuccessful()) {
            groups[response.body?.id!!] = response.body
            updates.value++
            null to response.body.id!!
        } else if (response.error?.equals("Group name is taken") == true) {
            "Group name is taken" to null
        } else {
            "Connection Error" to null
        }
    }

    suspend fun deleteGroup(groupId: UUID): Boolean {
        val result = Repository.deleteGroup(groupId)
        return if (result.isSuccessful()) {
            groups.remove(groupId)
            updates.value++
            true
        } else {
            QuizApplication.showSortToast("Error deleting group")
            false
        }
    }

    suspend fun changeGroupName(groupId: UUID, name:String): Boolean {
        val response = Repository.changeGroupName(groupId, name)
        return if (response.isSuccessful()) {
            groups[response.body?.id!!] = response.body
            updates.value++
            true
        } else {
            QuizApplication.showSortToast("Error deleting group")
            false
        }
    }

    suspend fun addUser(groupId: UUID, username: String): Boolean {
        val result = Repository.addUserToGroup(groupId, username)
        return if (result.isSuccessful()) {
            groups[result.body?.id!!] = result.body
            updates.value++
            true
        } else {
            QuizApplication.showSortToast("Error adding user")
            false
        }
    }

    suspend fun removeUser(groupId: UUID, username: String): Boolean {
        val result = Repository.removeUserFromGroup(groupId, username)
        return if (result.isSuccessful()) {
            groups[result.body?.id!!] = result.body
            updates.value++
            true
        } else {
            QuizApplication.showSortToast("Error removing user")
            false
        }
    }


    suspend fun addUserToWriters(groupId: UUID, username: String): Boolean {
        val result = Repository.addUserToGroupWriter(groupId, username)
        return if (result.isSuccessful()) {
            groups[result.body?.id!!] = result.body
            updates.value++
            true
        } else {
            QuizApplication.showSortToast("Error adding user to moderators")
            false
        }
    }

    suspend fun removeUserFromWriters(groupId: UUID, username: String): Boolean {
        val result = Repository.removeUserFromGroupWriter(groupId, username)
        return if (result.isSuccessful()) {
            groups[result.body?.id!!] = result.body
            updates.value++
            true
        } else {
            QuizApplication.showSortToast("Error removing user from moderators")
            false
        }
    }
}