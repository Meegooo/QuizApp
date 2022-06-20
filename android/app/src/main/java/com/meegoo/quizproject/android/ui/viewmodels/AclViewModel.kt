package com.meegoo.quizproject.android.ui.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.NetworkResponse
import com.meegoo.quizproject.android.data.Repository
import com.meegoo.quizproject.android.data.dto.AclDto
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*


class AclViewModel : ViewModel() {
    var id = mutableStateOf<UUID?>(null)
    var accessors = mutableStateListOf<AclDto>()

    var groupsExpanded = mutableStateOf(true)
    var usersExpanded = mutableStateOf(true)

    var loadingData = mutableStateOf(false)
    var loadingError = mutableStateOf(false)
    var isOwner = mutableStateOf(false)

    var aclType: AclType? = null
    private set

    var updates = mutableStateOf(0)

    fun loadFromQuizId(id: UUID) {
        aclType = AclType.QUIZ
        loadResults(id) {
            Repository.findAllAccessorsForQuiz(id)
        }
        updates.value++
    }

    fun loadFromCourseId(id: UUID) {
        aclType = AclType.COURSE
        loadResults(id) {
            Repository.findAllAccessorsForCourse(id)
        }
        updates.value++
    }

    fun reload() {
        val typeCopy = aclType
        val idCopy = id.value
        if (typeCopy == null || idCopy == null) {
            Log.e("AclViewModel", "Can't reload ACL because it's not initialized")
        } else {
            when (aclType) {
                AclType.QUIZ -> loadFromQuizId(idCopy)
                AclType.COURSE -> loadFromCourseId(idCopy)
            }
        }
        updates.value++
    }

    suspend fun createUser(user: String): String {
        val error = modifyPrivileges("user:$user", GrantedPermission.READ)
        val s = when {
            error == "Sid not found" -> "User not found"
            error != null -> {
                QuizApplication.showSortToast("Error adding user ACL")
                ""
            }
            else -> ""
        }
        updates.value++
        return s
    }

    suspend fun createGroup(group: String): String {
        val error = modifyPrivileges("group:$group", GrantedPermission.READ)
        val s = when {
            error == "Sid not found" -> "Group not found"
            error != null -> {
                QuizApplication.showSortToast("Error adding group ACL")
                ""
            }
            else -> ""
        }

        updates.value++
        return s
    }

    fun deleteUser(user: String) {
        viewModelScope.launch {
            val sid = "user:$user"
            if (modifyPrivileges(sid, GrantedPermission.NONE) != null) {
                QuizApplication.showSortToast("Error deleting ACL")
            } else {
                accessors.removeAll { it.sid == sid }
            }

            updates.value++
        }
    }

    fun deleteGroup(group: String) {
        val sid = "group:$group"
        viewModelScope.launch {
            if (modifyPrivileges(sid, GrantedPermission.NONE) != null) {
                QuizApplication.showSortToast("Error deleting ACL")
            } else {
                accessors.removeAll {it.sid == sid}
            }

            updates.value++
        }
    }

    fun modifyUser(user: String, privilege: GrantedPermission) {
        viewModelScope.launch {
            if (modifyPrivileges("user:$user", privilege) != null) {
                QuizApplication.showSortToast("Error modifying ACL")
            }

            updates.value++
        }
    }

    fun modifyGroup(group: String, privilege: GrantedPermission) {
        viewModelScope.launch {
            if (modifyPrivileges("group:$group", privilege) != null) {
                QuizApplication.showSortToast("Error modifying ACL")
            }

            updates.value++
        }
    }

    private suspend fun modifyPrivileges(sid: String, newPrivilege: GrantedPermission): String? {
        loadingData.value = true
        val typeCopy = aclType
        val idCopy = id.value
        if (typeCopy == null || idCopy == null) {
            Log.e("AclViewModel", "Can't reload ACL because it's not initialized")
            loadingData.value = false
            return null
        } else {
            val result = when (typeCopy) {
                AclType.QUIZ -> {
                    when (newPrivilege) {
                        GrantedPermission.NONE -> {
                            grantRevoke(idCopy, sid, null, GrantedPermission.READ, true)
                        }
                        GrantedPermission.READ -> {
                            grantRevoke(idCopy, sid, GrantedPermission.READ, GrantedPermission.SHARE, true)
                        }
                        GrantedPermission.SHARE -> {
                            grantRevoke(idCopy, sid, GrantedPermission.SHARE, GrantedPermission.WRITE, true)
                        }
                        GrantedPermission.WRITE -> {
                            grantRevoke(idCopy, sid, GrantedPermission.WRITE, GrantedPermission.ADMINISTRATION, true)
                        }
                        GrantedPermission.ADMINISTRATION -> {
                            grantRevoke(idCopy, sid, GrantedPermission.ADMINISTRATION, null, true)
                        }
                        else -> {
                            QuizApplication.showSortToast("Unknown privilege")
                            null
                        }
                    }
                }
                AclType.COURSE -> {
                    when (newPrivilege) {

                        GrantedPermission.NONE -> {
                            grantRevoke(idCopy, sid, null, GrantedPermission.READ, false)
                        }
                        GrantedPermission.READ -> {
                            grantRevoke(idCopy, sid, GrantedPermission.READ, GrantedPermission.SHARE, false)
                        }
                        GrantedPermission.WRITE -> {
                            grantRevoke(idCopy, sid, GrantedPermission.WRITE, GrantedPermission.ADMINISTRATION, false)
                        }
                        GrantedPermission.ADMINISTRATION -> {
                            grantRevoke(idCopy, sid, GrantedPermission.ADMINISTRATION, null, false)
                        }
                        else -> {
                            QuizApplication.showSortToast("Unknown privilege")
                            null
                        }
                    }
                }
            }
            loadingData.value = false
            return result
        }
    }

    private suspend fun grantRevoke(id: UUID, sid: String, grant: GrantedPermission?, revoke: GrantedPermission?, isQuiz: Boolean): String? {
        val revokeError = if (revoke != null) {
            val result = if (isQuiz) {
                    Repository.revokeQuiz(id, AclDto(sid, revoke))
                } else {
                    Repository.revokeCourse(id, AclDto(sid, revoke))
                }
            if (result.isSuccessful()) {
                val idx = accessors.indexOfFirst { it.sid == sid }
                if (idx == -1) {
                    accessors.add(result.body!!)
                } else {
                    accessors[idx] = result.body!!
                }
                null
            } else {
                result.error ?: ""
            }
        } else {
            null
        }

        if (revokeError == null) {
            return if (grant != null) {
                val result = if (isQuiz) {
                    Repository.grantQuiz(id, AclDto(sid, grant))
                } else {
                    Repository.grantCourse(id, AclDto(sid, grant))
                }
                if (result.isSuccessful()) {
                    val idx = accessors.indexOfFirst { it.sid == sid }
                    if (idx == -1) {
                        accessors.add(result.body!!)
                    } else {
                        accessors[idx] = result.body!!
                    }
                    null
                } else {
                    result.error ?: ""
                }
            } else {
                null
            }
        } else {
            return revokeError
        }
    }

    private fun loadResults(id: UUID, loader: suspend (UUID) -> NetworkResponse<List<AclDto>>) {
        loadingData.value = true
        loadingError.value = false
        this@AclViewModel.id.value = id
        viewModelScope.launch {
            val response = loader(id)
            if (response.isSuccessful()) {
                val list = response.body
                accessors.clear()
                accessors.addAll(list!!)
            } else {
                loadingError.value = true
            }
            loadingData.value = false
            updates.value++
        }
    }



    fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

    enum class AclType {
        QUIZ, COURSE
    }
}