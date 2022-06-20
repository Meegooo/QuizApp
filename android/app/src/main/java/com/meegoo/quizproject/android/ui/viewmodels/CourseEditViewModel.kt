package com.meegoo.quizproject.android.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.dto.CourseDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import java.util.*


class CourseEditViewModel : ViewModel() {
    var id = mutableStateOf<UUID?>(null)
    var name = mutableStateOf("")
    var quizzes = mutableStateListOf<QuizDto>()
    var permissions = mutableStateListOf<GrantedPermission>()

    var addedQuizzes = mutableStateListOf<QuizDto>()
    var removedQuizzes = mutableStateListOf<UUID>()
    var awaitResult = false


    fun fromCourseDto(courseDto: CourseDto, mainViewModel: MainViewModel) {
        this.id.value = courseDto.id
        this.name.value = courseDto.name ?: ""
        this.quizzes.clear()
        this.quizzes.addAll(courseDto.quizzes.mapNotNull { mainViewModel.getQuiz(it.id!!).value })
        this.permissions.clear()
        this.permissions.addAll(courseDto.permissions)
    }

    fun toCourseDto(): CourseDto {
        val quiz = quizzes.map { QuizDto(it.id) }.toMutableSet()
        return CourseDto(id.value, name.value, quiz)
    }


    var updatingCourse = mutableStateOf(false)
    var updatingCourseError = mutableStateOf(false)

    suspend fun createCourse(mainViewModel: MainViewModel) {
        if (!updatingCourse.value) {
            updatingCourse.value = true
            updatingCourseError.value = false
            val (createdCourse, successfulCreate) = mainViewModel.createCourse(name.value)
            if (successfulCreate && createdCourse.value != null) {
                id.value = createdCourse.value!!.id
            } else {
                updatingCourseError.value = true
                QuizApplication.showSortToast("Error creating course")
            }
            updatingCourse.value = false

        }
    }

    suspend fun updateCourse(mainViewModel: MainViewModel) {
        if (!updatingCourse.value) {
            updatingCourse.value = true
            updatingCourseError.value = false
            val (updatedCourse, successUpdate) = mainViewModel.updateCourseName(CourseDto(id.value!!, name.value))
            val successRemoving: Boolean = if (successUpdate) {
                deleteQuizFromCourse(mainViewModel)
            } else false
            val successAdding: Boolean = if (successRemoving) {
                addQuizToCourse(mainViewModel)
            } else false
            if (successAdding && updatedCourse.value != null) {
                id.value = updatedCourse.value!!.id
            } else {
                updatingCourseError.value = true
                QuizApplication.showSortToast("Error updating course")
            }
            updatingCourse.value = false

        }
    }

    suspend fun deleteCourse(mainViewModel: MainViewModel) {
        if (!updatingCourse.value) {
            updatingCourse.value = true
            val successful = mainViewModel.deleteCourse(id.value!!)
            if (!successful) {
                QuizApplication.showSortToast("Error deleting course")
            }
            updatingCourse.value = false

        }
    }

    fun addQuiz(mainViewModel: MainViewModel, quizUuid: UUID) {
        if (!addedQuizzes.any { it.id == quizUuid })
            addedQuizzes.add(mainViewModel.getQuiz(quizUuid).value!!)
    }

    fun removeQuiz(quizUuid: UUID) {
        if (!addedQuizzes.removeAll { it.id == quizUuid }) {
            removedQuizzes.add(quizUuid)
        }
    }

    private suspend fun addQuizToCourse(mainViewModel: MainViewModel): Boolean {
        addedQuizzes.forEach {
            val (updatedCourse, successfulAdd) = mainViewModel.addQuizToCourse(id.value!!, it.id!!)
            if (successfulAdd && updatedCourse.value != null) {
                id.value = updatedCourse.value!!.id
                quizzes.add(it)
            } else {
                return false
            }
        }
        return true
    }

    private suspend fun deleteQuizFromCourse(mainViewModel: MainViewModel): Boolean {
        removedQuizzes.forEach { uuid ->
            val (updatedCourse, successfulAdd) = mainViewModel.deleteQuizFromCourse(id.value!!, uuid)
            if (successfulAdd && updatedCourse.value != null) {
                id.value = updatedCourse.value!!.id
                quizzes.removeAll { it.id == uuid }
            } else {
                return false
            }
        }
        return true
    }
}
