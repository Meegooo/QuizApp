package com.meegoo.quizproject.android.ui.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meegoo.quizproject.android.data.NetworkResponse
import com.meegoo.quizproject.android.data.Repository
import com.meegoo.quizproject.android.data.dto.CourseDto
import com.meegoo.quizproject.android.data.dto.LoadLevel
import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel : ViewModel() {

    private val quizCache = mutableStateMapOf<UUID, MutableState<QuizDto?>>()
    private val quizAttemptCache = mutableStateMapOf<UUID, MutableState<QuizAttemptDto?>>()
    private val courseCache = mutableStateMapOf<UUID, MutableState<CourseDto?>>()

    val quizListRefreshing = mutableStateOf(false)
    val quizOverviewRefreshing = mutableStateOf(false)
    val loadingQuiz = mutableStateOf(false)

    val courseExpanded = mutableStateMapOf<UUID?, Boolean>()

    var quizCourseUpdates = mutableStateOf(0)
    val errors = RequestErrors()

    suspend fun loadQuizOverview(force: Boolean = false) {
        if (force || quizCache.isEmpty()) {
            quizListRefreshing.value = true
            errors.loadQuizOverviewError.value = null
            quizCache.clear()
            courseCache.clear()
            val quizResponse = Repository.loadQuizzesOverview()
            val courseResponse = Repository.loadCourses()
            if (quizResponse.isSuccessful() && courseResponse.isSuccessful()) {
                quizCache.putAll(
                    quizResponse.body!!.mapValues { mutableStateOf(it.value) })
                courseCache.putAll(
                    courseResponse.body!!.mapValues { mutableStateOf(it.value) })
            } else {
                errors.loadQuizOverviewError.value = quizResponse.error ?: "Unknown Error"
            }
            quizListRefreshing.value = false
            quizCourseUpdates.value++
        }

    }

    suspend fun loadQuiz(uuid: UUID, force: Boolean = false): MutableState<QuizDto?> {
        loadingQuiz.value = true
        val state = quizCache.computeIfAbsent(uuid) { mutableStateOf(null) }
        errors.loadQuizError.value = null
        if (force ||
            state.value == null ||
            state.value!!.loadLevel.level < LoadLevel.READ.level
        ) {
            val response: NetworkResponse<QuizDto> = Repository.loadQuiz(uuid)
            if (response.isSuccessful()) {
                state.value = response.body!!
            } else {
                errors.loadQuizError.value = response.error
            }
        }
        quizCourseUpdates.value++
        loadingQuiz.value = false
        return state
    }

    fun loadQuizEditable(uuid: UUID, force: Boolean = false): MutableState<QuizDto?> {
        loadingQuiz.value = true
        val state = quizCache.computeIfAbsent(uuid) { mutableStateOf(null) }
        viewModelScope.launch {
            errors.loadQuizError.value = null
            if (force ||
                state.value == null ||
                state.value!!.loadLevel.level < LoadLevel.WRITE.level
            ) {
                val response: NetworkResponse<QuizDto> = Repository.loadQuiz(uuid, true)
                if (response.isSuccessful()) {
                    state.value = response.body!!
                } else {
                    errors.loadQuizError.value = response.error
                }
            }
            quizCourseUpdates.value++
            loadingQuiz.value = false
        }
        return state
    }

    fun getQuiz(uuid: UUID): MutableState<QuizDto?> {
        return quizCache.computeIfAbsent(uuid) { mutableStateOf(null) }
    }

    suspend fun createQuiz(name: String): Pair<MutableState<QuizDto?>, Boolean> {
        val response = Repository.createQuiz(name)
        return if (response.isSuccessful()) {
            val mutableStateOf: MutableState<QuizDto?> = mutableStateOf(response.body)
            quizCache[response.body?.id!!] = mutableStateOf
            quizCourseUpdates.value++
            mutableStateOf to true
        } else {
            quizCourseUpdates.value++
            mutableStateOf<QuizDto?>(null) to false
        }

    }

    suspend fun exportQuiz(uuid: UUID): QuizDto? {
        val response = Repository.exportQuiz(uuid)
        return if (response.isSuccessful()) {
            response.body
        } else {
            null
        }
    }

    suspend fun importQuiz(quiz: QuizDto): QuizDto? {
        val response = Repository.importQuiz(quiz)
        return if (response.isSuccessful()) {
            val mutableStateOf: MutableState<QuizDto?> = mutableStateOf(response.body)
            quizCache[response.body?.id!!] = mutableStateOf
            quizCourseUpdates.value++
            response.body
        } else {
            null
        }
    }

    suspend fun updateQuiz(quizDto: QuizDto): Pair<MutableState<QuizDto?>, Boolean> {
        val state = quizCache[quizDto.id]!!
        val response = Repository.updateQuiz(quizDto)
        return if (response.isSuccessful()) {
            quizCache[response.body?.id!!]!!.value = response.body
            quizCourseUpdates.value++
            state to true
        } else {
            quizCourseUpdates.value++
            state to false
        }
    }

    suspend fun publishQuiz(id: UUID?): Pair<MutableState<QuizDto?>, Boolean> {
        val state = quizCache[id]!!
        val response = Repository.publishQuiz(id)
        return if (response.isSuccessful()) {
            quizCache[response.body?.id!!]!!.value = response.body
            quizCourseUpdates.value++
            state to true
        } else {
            quizCourseUpdates.value++
            state to false
        }

    }

    suspend fun deleteQuiz(id: UUID): Boolean {
        val response = Repository.deleteQuiz(id)
        return if (response.isSuccessful()) {
            quizCache.remove(id)
            quizCourseUpdates.value++
            true
        } else {
            quizCourseUpdates.value++
            false
        }
    }


    fun quizzes(): Pair<Map<CourseDto, List<QuizDto>>, List<QuizDto>> {
        return quizList(quizCache)
    }

    fun quizzesShareable(): List<QuizDto> {
        return quizCache.filter { it.value.value?.permissions?.contains(GrantedPermission.SHARE) ?: false }.map { it.value.value!! }
    }

    fun quizzesEditable(): Pair<Map<CourseDto, List<QuizDto>>, List<QuizDto>> {
        return quizList(quizCache, GrantedPermission.WRITE)
    }

    private fun quizList(list: Map<UUID, MutableState<QuizDto?>>, requirePermission: GrantedPermission = GrantedPermission.READ): Pair<Map<CourseDto, List<QuizDto>>, List<QuizDto>> {
        val courses = courseCache
            .mapKeys { it.value.value!! }
            .mapValues {
                it.value.value?.quizzes
                    ?.asSequence()
                    ?.mapNotNull { list[it.id]?.value }
                    ?.filter { requirePermission in it.permissions }
                    ?.sortedBy { it.name }
                    ?.toList() ?: emptyList()
            }
            .toSortedMap(compareBy { it.name })

        val allQuizzesInCourses = courseCache.flatMap { it.value.value?.quizzes?.map { it.id } ?: emptyList() }.toSet()
        val quizzesNotInCourses =
            list.values
                .asSequence()
                .filter { it.value != null }
                .filter { requirePermission in it.value?.permissions ?: emptyList() }
                .filterNot { it.value?.id in allQuizzesInCourses }.map { it.value!! }
                .sortedBy { it.name }
                .toList()
        return courses to quizzesNotInCourses
    }

    suspend fun loadQuizAttempt(quizUuid: UUID, force: Boolean = false): MutableState<QuizAttemptDto?> {
        val state = quizAttemptCache.computeIfAbsent(quizUuid) { mutableStateOf(null) }
        errors.loadQuizAttemptError.value = null
        if (force || state.value == null) {
            val response = Repository.loadQuizAttempt(quizUuid)
            if (response.isSuccessful() && response.body != null) {
                state.value = response.body
            } else if (response.isSuccessful() && response.body == null) {
                state.value = QuizAttemptDto()
            } else {
                errors.loadQuizAttemptError.value = response.error
            }
        }
        return state
    }

    fun getQuizAttempt(quizUuid: UUID): MutableState<QuizAttemptDto?> {
        return quizAttemptCache.computeIfAbsent(quizUuid) { mutableStateOf(null) }
    }

    suspend fun createQuizAttempt(quizUuid: UUID, force: Boolean = false): MutableState<QuizAttemptDto?> {
        val state = quizAttemptCache.computeIfAbsent(quizUuid) { mutableStateOf(null) }
        errors.createQuizAttemptError.value = null
        if (force ||
            state.value == null ||
            state.value!!.loadLevel.level < LoadLevel.READ.level
        ) {
            val response = Repository.createAttempt(quizUuid)
            if (response.isSuccessful()) {
                state.value = response.body
            } else {
                errors.createQuizAttemptError.value = response.error
            }
        }
        return state
    }

    suspend fun putQuizAttempt(quizAttemptDto: QuizAttemptDto, quizUuid: UUID): Pair<QuizAttemptDto?, Boolean> {
        val answer = Repository.putAttempt(quizAttemptDto)
        return if (answer.isSuccessful()) {
            quizAttemptCache[quizUuid]?.value = answer.body
            answer.body!! to true
        } else {
            null to false
        }
    }

    suspend fun finishAttempt(quizAttempt: QuizAttemptDto, quizUuid: UUID): Pair<QuizAttemptDto?, Boolean> {
        val answer = Repository.finishAttempt(quizAttempt)
        return if (answer.isSuccessful()) {
            quizAttemptCache[quizUuid]?.value = answer.body
            answer.body!! to true
        } else {
            null to false
        }
    }

    fun loadCourse(uuid: UUID, force: Boolean = false): MutableState<CourseDto?> {
        val state = courseCache.computeIfAbsent(uuid) { mutableStateOf(null) }
        viewModelScope.launch {
            errors.loadCourseError.value = null
            if (force || state.value == null) {
                val response = Repository.loadCourse(uuid)
                if (response.isSuccessful()) {
                    state.value = response.body!!
                } else {
                    errors.loadCourseError.value = response.error
                }
            }
            quizCourseUpdates.value++
        }
        return state
    }

    fun getCourse(uuid: UUID): MutableState<CourseDto?> {
        return courseCache.computeIfAbsent(uuid) { mutableStateOf(null) }
    }

    suspend fun createCourse(name: String): Pair<MutableState<CourseDto?>, Boolean> {
        val response = Repository.createCourse(name)
        return if (response.isSuccessful()) {
            val mutableStateOf: MutableState<CourseDto?> = mutableStateOf(response.body)
            courseCache[response.body?.id!!] = mutableStateOf
            quizCourseUpdates.value++
            mutableStateOf to true
        } else {
            quizCourseUpdates.value++
            mutableStateOf<CourseDto?>(null) to false
        }
    }

    suspend fun updateCourseName(courseDto: CourseDto): Pair<MutableState<CourseDto?>, Boolean> {
        val response = Repository.updateCourseName(courseDto)
        return if (response.isSuccessful()) {
            val mutableStateOf: MutableState<CourseDto?> = mutableStateOf(response.body)
            courseCache[response.body?.id!!] = mutableStateOf
            quizCourseUpdates.value++
            mutableStateOf to true
        } else {
            quizCourseUpdates.value++
            mutableStateOf<CourseDto?>(null) to false
        }
    }

    suspend fun addQuizToCourse(courseUuid: UUID, quizUuid: UUID): Pair<MutableState<CourseDto?>, Boolean> {
        val response = Repository.addQuizToCourse(courseUuid, quizUuid)
        return if (response.isSuccessful()) {
            val mutableStateOf: MutableState<CourseDto?> = mutableStateOf(response.body)
            courseCache[response.body?.id!!] = mutableStateOf
            quizCourseUpdates.value++
            mutableStateOf to true
        } else {
            quizCourseUpdates.value++
            mutableStateOf<CourseDto?>(null) to false
        }
    }

    suspend fun deleteQuizFromCourse(courseUuid: UUID, quizUuid: UUID): Pair<MutableState<CourseDto?>, Boolean> {
        val response = Repository.deleteQuizFromCourse(courseUuid, quizUuid)
        return if (response.isSuccessful()) {
            val mutableStateOf: MutableState<CourseDto?> = mutableStateOf(response.body)
            courseCache[response.body?.id!!] = mutableStateOf
            quizCourseUpdates.value++
            mutableStateOf to true
        } else {
            quizCourseUpdates.value++
            mutableStateOf<CourseDto?>(null) to false
        }
    }

    suspend fun deleteCourse(id: UUID): Boolean {
        val response = Repository.deleteCourse(id)
        return if (response.isSuccessful()) {
            courseCache.remove(id)
            quizCourseUpdates.value++
            true
        } else {
            quizCourseUpdates.value++
            false
        }
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

    fun clearQuizAttempts() {
        quizAttemptCache.clear()
    }

    class RequestErrors {
        var loadQuizOverviewError = mutableStateOf<String?>(null)
        var loadQuizOverviewEditError = mutableStateOf<String?>(null)
        var loadQuizError = mutableStateOf<String?>(null)
        var postQuizAttemptError = mutableStateOf<String?>(null)
        var createQuizAttemptError = mutableStateOf<String?>(null)
        var loadQuizAttemptError = mutableStateOf<String?>(null)
        var loadCourseError = mutableStateOf<String?>(null)
    }
}