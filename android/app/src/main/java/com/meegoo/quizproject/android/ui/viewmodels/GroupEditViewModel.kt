package com.meegoo.quizproject.android.ui.viewmodels

import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.Repository
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.server.data.dto.GroupDto
import kotlinx.coroutines.launch
import java.util.*
import java.util.function.Function
import kotlin.collections.HashMap
import kotlin.collections.HashSet


class GroupEditViewModel : ViewModel() {
    var id = mutableStateOf<UUID?>(null)
    var name = mutableStateOf("")
    private var users = mutableStateMapOf<String, GrantedPermission>()
    var permissions: List<GrantedPermission> = emptyList()

    private var addedUsers = mutableStateMapOf<String, GrantedPermission>()
    private var removedUsers = mutableStateListOf<String>()

    var addedWrite = mutableStateListOf<String>()
    var removedWrite = mutableStateListOf<String>()

    val updates = mutableStateOf(0)
    val updating = mutableStateOf(false)

    fun fromGroupViewModel(uuidString: String, groupViewModel: GroupViewModel) {
        if (uuidString == "new") {
            this.id.value = null
            this.name.value = ""
            this.users.clear()
            this.permissions = listOf(GrantedPermission.WRITE, GrantedPermission.OWNER)
        } else {
            val uuid = UUID.fromString(uuidString)
            fromGroupDto(groupViewModel.groups[uuid]!!)
        }
        updates.value++
    }

    fun fromGroupDto(groupDto: GroupDto) {
        this.id.value = groupDto.id
        this.name.value = groupDto.name!!
        this.permissions = groupDto.permissions
        this.users.clear()
        this.users.putAll(groupDto.users)
        updates.value++

    }

    fun getUsersSorted(): List<Pair<String, GrantedPermission>> {
        return HashMap<String, GrantedPermission>(users + addedUsers)
            .filter { it.key !in removedUsers }
            .entries.sortedWith(Comparator
                .comparing<Map.Entry<String, GrantedPermission>, Int> { it.value.level }
                .reversed()
                .thenComparing(Function { it.key })
            ).map { it.key to it.value }
            .map {
                when (it.first) {
                    in addedWrite -> it.first to GrantedPermission.WRITE
                    in removedWrite -> it.first to GrantedPermission.READ
                    else -> it.first to it.second
                }
            }
    }

    suspend fun addUser(value: String): String? {
        if (value in removedUsers) {
            removedUsers.remove(value)
            updates.value++
            return null
        } else {
            val check = Repository.checkUsernameExists(value)
            val result = if (check.isSuccessful() && check.body == true) {
                addedUsers.put(value, GrantedPermission.READ)
                null
            } else if (check.isSuccessful() && check.body == false) {
                "User not found"
            } else {
                "Connection error"
            }
            updates.value++
            return result
        }
    }

    fun removeUser(value: String) {
        viewModelScope.launch {
            if (value in addedUsers.keys) {
                addedUsers.remove(value)
            } else {
                removedUsers.add(value)
            }
            updates.value++
        }
    }

    suspend fun createGroup(groupViewModel: GroupViewModel): Boolean {
        updating.value = true
        val creationResult = groupViewModel.createGroup(name.value)
        val result = if (creationResult.second != null) {
            id.value = creationResult.second
            addedUsers.keys.forEach {
                if (!groupViewModel.addUser(id.value!!, it)) {
                    fromGroupViewModel(creationResult.second.toString(), groupViewModel)
                    return@forEach
                }
            }
            addedWrite.forEach {
                if (!groupViewModel.addUserToWriters(id.value!!, it)) {
                    fromGroupViewModel(id.value.toString(), groupViewModel)
                    return@forEach
                }
            }
            true
        } else {
            QuizApplication.showSortToast(creationResult.first ?: "")
            false
        }

        updating.value = false
        updates.value++
        return result
    }

    suspend fun updateGroup(groupViewModel: GroupViewModel): Boolean {
        updating.value = true
        var result = true
        if (!groupViewModel.changeGroupName(id.value!!, name.value)) {
            result = false
        }
        if (result) addedUsers.keys.forEach {
            if (!groupViewModel.addUser(id.value!!, it)) {
                fromGroupViewModel(id.value.toString(), groupViewModel)
                result = false
                return@forEach
            }
        }
        if (result) removedUsers.forEach {
            if (!groupViewModel.removeUser(id.value!!, it)) {
                fromGroupViewModel(id.value.toString(), groupViewModel)
                result = false
                return@forEach
            }
        }

        if (result) addedWrite.forEach {
            if (!groupViewModel.addUserToWriters(id.value!!, it)) {
                fromGroupViewModel(id.value.toString(), groupViewModel)
                result = false
                return@forEach
            }
        }

        if (result) removedWrite.forEach {
            if (!groupViewModel.removeUserFromWriters(id.value!!, it)) {
                fromGroupViewModel(id.value.toString(), groupViewModel)
                result = false
                return@forEach
            }
        }

        updating.value = false
        updates.value++
        return result
    }

    fun permissionChange(name: String, hasWrite: Boolean) {
        if (hasWrite) {
            if (name in removedWrite) {
                removedWrite.remove(name)
            } else {
                addedWrite.add(name)
            }
        } else {
            if (name in addedWrite) {
                addedWrite.remove(name)
            } else {
                removedWrite.add(name)
            }
        }
        updates.value++
    }

}