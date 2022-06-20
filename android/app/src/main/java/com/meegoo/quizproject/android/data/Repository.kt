package com.meegoo.quizproject.android.data

import android.util.Log
import com.meegoo.quizproject.android.data.dto.CourseDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.network.api.RequestController
import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import com.meegoo.quizproject.android.data.dto.AclDto
import com.meegoo.quizproject.server.data.dto.GroupDto
import retrofit2.http.Path
import java.lang.RuntimeException
import java.util.*

object Repository {
    private val quizService = RequestController.quizService
    private val quizAttemptService = RequestController.quizAttemptService
    private val courseService = RequestController.courseService
    private val aclService = RequestController.aclService
    private val groupService = RequestController.groupService


    suspend fun loadQuizzesOverview(): NetworkResponse<Map<UUID, QuizDto>> {
        val response = quizService.getOverview()
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body.associateBy { it.id!! })
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun loadQuiz(uuid: UUID, editable: Boolean = false): NetworkResponse<QuizDto> {
        val response = if (!editable) quizService.getQuiz(uuid) else quizService.getQuizEditable(uuid)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            body.questions.sortBy { it.index }
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }

    }

    suspend fun createQuiz(name: String): NetworkResponse<QuizDto> {
        val response = quizService.createQuiz(name)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            body.questions.sortBy { it.index }
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun updateQuiz(quizDto: QuizDto): NetworkResponse<QuizDto> {
        if (quizDto.id == null) return NetworkResponse("Quiz not created")

        val response = quizService.updateQuiz(quizDto.id!!, quizDto)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            body.questions.sortBy { it.index }
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun publishQuiz(id: UUID?): NetworkResponse<QuizDto> {
        if (id == null) return NetworkResponse("Quiz not created")

        val response = quizService.publishQuiz(id)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            body.questions.sortBy { it.index }
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun deleteQuiz(id: UUID): NetworkResponse<Unit> {
        val response = quizService.deleteQuiz(id)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun exportQuiz(id: UUID): NetworkResponse<QuizDto> {
        val response = quizService.exportQuiz(id)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun importQuiz(quiz: QuizDto): NetworkResponse<QuizDto> {
        val response = quizService.importQuiz(quiz)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun loadQuizAttempt(quizUuid: UUID): NetworkResponse<QuizAttemptDto?> {
        Log.i("Attempts", "Loading attempt for quiz $quizUuid")
        val response = quizAttemptService.getQuizAttempts(quizUuid)
        val body = response.body()
        return if (response.isSuccessful && body != null && body.isNotEmpty()) {
            val attempt = quizAttemptService.getAttempt(body[0].id!!)
            val attemptBody = attempt.body()
            if (attempt.isSuccessful && attemptBody != null) {
                NetworkResponse(attemptBody)
            } else {
                NetworkResponse(attempt.message())
            }
        } else if (response.isSuccessful && body != null) {
            NetworkResponse(null)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun createAttempt(quizUuid: UUID): NetworkResponse<QuizAttemptDto> {
        Log.i("Attempts", "Creating attempt for quiz $quizUuid")
        val response = quizAttemptService.createAttempt(quizUuid)
        return if (response.isSuccessful && response.body() != null) {
            NetworkResponse(response.body()!!)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun putAttempt(attempt: QuizAttemptDto): NetworkResponse<QuizAttemptDto> {
        Log.i("Attempts", "Sending attempt for quiz ${attempt.quizId}")
        val response = quizAttemptService.putAttempt(attempt.id!!, attempt)
        return if (response.isSuccessful && response.body() != null) {
            NetworkResponse(response.body()!!)
        } else if (response.errorBody()?.string()?.contains("Attempt is closed") == true) {
            Log.e("Attempts", "Attempt for quiz ${attempt.quizId} is closed")
            NetworkResponse("Attempt is closed")
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun finishAttempt(attempt: QuizAttemptDto): NetworkResponse<QuizAttemptDto> {
        Log.i("Attempts", "Finishing attempt for quiz ${attempt.quizId}")
        val response = quizAttemptService.finishAttempt(attempt.id!!)
        return if (response.isSuccessful && response.body() != null) {
            NetworkResponse(response.body()!!)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun loadCourse(uuid: UUID): NetworkResponse<CourseDto> {
        val response = courseService.getCourse(uuid)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun loadCourses(): NetworkResponse<Map<UUID, CourseDto>> {
        val response = courseService.getCourses()
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body.associateBy { it.id!! })
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun createCourse(name: String): NetworkResponse<CourseDto> {
        val response = courseService.createCourse(name)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun deleteCourse(courseId: UUID): NetworkResponse<Unit> {
        val response = courseService.deleteCourse(courseId)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun updateCourseName(courseDto: CourseDto): NetworkResponse<CourseDto> {
        if (courseDto.id == null) return NetworkResponse("Course not created")

        val response = courseService.updateCourse(courseDto.id!!, courseDto.name!!)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun addQuizToCourse(courseId: UUID, quizId: UUID): NetworkResponse<CourseDto> {
        val response = courseService.addQuizToCourse(courseId, quizId)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }
    suspend fun deleteQuizFromCourse(courseId: UUID, quizId: UUID): NetworkResponse<CourseDto> {
        val response = courseService.deleteQuizFromCourse(courseId, quizId)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun grantQuiz(quizId: UUID, aclDto: AclDto): NetworkResponse<AclDto> {
        val response = aclService.grantQuiz(quizId, aclDto)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else if (response.errorBody()?.string()?.contains("Sid not found") == true){
            NetworkResponse("Sid not found")
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun revokeQuiz(quizId: UUID, aclDto: AclDto): NetworkResponse<AclDto> {
        val response = aclService.revokeQuiz(quizId, aclDto)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else if (response.errorBody()?.string()?.contains("Sid not found") == true){
            NetworkResponse("Sid not found")
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun grantCourse(courseId: UUID, aclDto: AclDto): NetworkResponse<AclDto> {
        val response = aclService.grantCourse(courseId, aclDto)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else if (response.errorBody()?.string()?.contains("Sid not found") == true){
            NetworkResponse("Sid not found")
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun revokeCourse(courseId: UUID, aclDto: AclDto): NetworkResponse<AclDto> {
        val response = aclService.revokeCourse(courseId, aclDto)

        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else if (response.errorBody()?.string()?.contains("Sid not found") == true){
            NetworkResponse("Sid not found")
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun findAllAccessorsForQuiz(quizId: UUID): NetworkResponse<List<AclDto>> {
        val response = aclService.findAllForQuiz(quizId)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun findAllAccessorsForCourse(courseId: UUID): NetworkResponse<List<AclDto>> {
        val response = aclService.findAllForCourse(courseId)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun loadGroups(): NetworkResponse<Map<UUID, GroupDto>> {
        val response = groupService.getAllGroups()
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body.associateBy { it.id!! })
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun createGroup(name: String): NetworkResponse<GroupDto> {
        val response = groupService.createGroup(name)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else if (response.errorBody()?.string()?.contains("Group name is taken") == true){
            NetworkResponse("Group name is taken")
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun deleteGroup(groupId: UUID): NetworkResponse<Unit> {
        val response = groupService.deleteGroup(groupId)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun changeGroupName(groupId: UUID, name: String): NetworkResponse<GroupDto> {
        val response = groupService.changeGroupName(groupId, name)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun addUserToGroup(groupId: UUID, username: String): NetworkResponse<GroupDto> {
        val response = groupService.addUserToGroup(groupId, username)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun removeUserFromGroup(groupId: UUID, username: String): NetworkResponse<GroupDto> {
        val response = groupService.deleteUserFromGroup(groupId, username)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }


    suspend fun addUserToGroupWriter(groupId: UUID, username: String): NetworkResponse<GroupDto> {
        val response = groupService.addUserToGroupWriter(groupId, username)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun removeUserFromGroupWriter(groupId: UUID, username: String): NetworkResponse<GroupDto> {
        val response = groupService.removeUserFromGroupWriter(groupId, username)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }

    suspend fun checkUsernameExists(username: String): NetworkResponse<Boolean> {
        val response = groupService.checkUsernameExists(username)
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            NetworkResponse(body)
        } else {
            NetworkResponse(response.message())
        }
    }
}

class NetworkResponse<T> {
    val body: T?
    val error: String?

    constructor(body: T) {
        this.body = body
        this.error = null
    }

    constructor(error: String) {
        this.error = error
        this.body = null
    }

    fun isSuccessful(): Boolean {
        return error == null
    }

    fun isError(): Boolean {
        return error != null
    }
}

class NetworkException(message: String) : RuntimeException(message)