package com.meegoo.quizproject.android.ui.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.android.data.dto.GrantedPermission
import java.time.Instant
import java.util.*
import kotlin.math.max


class QuizEditViewModel : ViewModel() {
    val permissions = mutableStateListOf<GrantedPermission>()
    var id = mutableStateOf<UUID?>(null)
    var name = mutableStateOf("")
    var score = mutableStateOf("")
    var automaticScore = mutableStateOf(true)
    var timeLimit = mutableStateOf(TimeLimit.zero(), referentialEqualityPolicy())
    var timeUnlimited = mutableStateOf(false)
    var questions = mutableStateListOf<QuestionDto>()
    var publishedAt = mutableStateOf<Instant?>(null)


    fun fromQuizDto(quizDto: QuizDto) {
        this.id.value = quizDto.id
        this.name.value = quizDto.name ?: ""
        this.timeLimit.value = TimeLimit.fromSeconds(quizDto.timeLimit ?: 0)
        this.automaticScore.value = quizDto.automaticScore ?: true
        this.timeUnlimited.value = (quizDto.timeLimit ?: 0) < 0
        this.questions.clear()
        this.questions.addAll(quizDto.questions)
        this.publishedAt.value = quizDto.publishedAt
        this.permissions.clear()
        this.permissions.addAll(quizDto.permissions)
    }

    fun getDtoTimeLimit(): Int {
        return if (timeUnlimited.value) -1
        else {
            timeLimit.value.toSeconds()
        }
    }

    fun toQuizDto(): QuizDto {
        var parsedScore = if (automaticScore.value) null
        else {
            val t = score.value.replace(",", ".").trim()
            t.toDoubleOrNull() ?: 0.0
        }
        if (parsedScore != null && parsedScore <= 0) {
            parsedScore = 0.0
        }

        return QuizDto(id.value, name.value, getDtoTimeLimit(), parsedScore, automaticScore.value, null, ArrayList(questions), mutableListOf())
    }

    var updatingQuiz = mutableStateOf(0)

    suspend fun createQuiz(
        mainViewModel: MainViewModel,
        onToQuestionEdit: (UUID) -> Unit,
    ) {
        if (updatingQuiz.value == 0 || updatingQuiz.value == -1) {
            updatingQuiz.value = 1
            val (createdQuiz, successfulCreate) = mainViewModel.createQuiz(name.value)
            if (successfulCreate && createdQuiz.value != null) {
                id.value = createdQuiz.value!!.id
                updatingQuiz.value = 2
                createdQuiz.value!!.timeLimit = getDtoTimeLimit()
                val (updateQuiz, successfulUpdate) = mainViewModel.updateQuiz(createdQuiz.value!!)
                if (successfulUpdate && updateQuiz.value != null) {
                    fromQuizDto(updateQuiz.value!!)
                    updatingQuiz.value = 0
                    onToQuestionEdit(id.value!!)
                } else {
                    updatingQuiz.value = -2
                    QuizApplication.showSortToast("Error updating quiz")
                }
            } else {
                updatingQuiz.value = -1
                QuizApplication.showSortToast("Error creating quiz")
            }

        } else if (updatingQuiz.value == -2) {
            updatingQuiz.value = 2
            val (updateQuiz, success) = mainViewModel.updateQuiz(toQuizDto())
            if (updateQuiz.value != null && success) {
                fromQuizDto(updateQuiz.value!!)
                updatingQuiz.value = 0
                onToQuestionEdit(updateQuiz.value!!.id!!)
            } else {
                updatingQuiz.value = -2
                QuizApplication.showSortToast("Error updating quiz")
            }
        }
    }

    suspend fun updateQuiz(mainViewModel: MainViewModel) {
        if (updatingQuiz.value == 0 || updatingQuiz.value == -2) {
            updatingQuiz.value = 2
            val (updateQuiz, success) = mainViewModel.updateQuiz(toQuizDto())
            if (updateQuiz.value != null && success) {
                fromQuizDto(updateQuiz.value!!)
                updatingQuiz.value = 0
            } else {
                updatingQuiz.value = -2
                QuizApplication.showSortToast("Error updating quiz")
            }
        }
    }

    var publishingQuiz = mutableStateOf(false)
    var publishQuizError = mutableStateOf(false)

    suspend fun publishQuiz(mainViewModel: MainViewModel) {
        if (!publishingQuiz.value) {
            publishQuizError.value = false
            publishingQuiz.value = true
            val (updateQuiz, success) = mainViewModel.publishQuiz(id.value)
            if (updateQuiz.value != null && success) {
                fromQuizDto(updateQuiz.value!!)
                publishQuizError.value = false
            } else {
                publishQuizError.value = true
                QuizApplication.showSortToast("Error publishing quiz")
            }
            publishingQuiz.value = false
        }
    }

    var deletingQuiz = mutableStateOf(false)
    suspend fun deleteQuiz(mainViewModel: MainViewModel): Boolean {
        if (!deletingQuiz.value) {
            deletingQuiz.value = true
            val updateQuiz = mainViewModel.deleteQuiz(id.value!!)
            deletingQuiz.value = false
            if (!updateQuiz) {
                QuizApplication.showSortToast("Error deleting quiz")
            }
            return updateQuiz
        }
        return false
    }

    fun deleteQuestion(questionUuid: UUID): Boolean {
        val find = questions.indexOfFirst { it.id == questionUuid }
        if (find >= 0) {
            questions.removeAt(find)
            return true
        }
        return false
    }

    fun saveQuestion(questionEditViewModel: QuestionEditViewModel) {
        val find = questions.indexOfFirst { it.id == questionEditViewModel.id.value }
        val questionDto = questionEditViewModel.toQuestionDto()

        //Calculate
        if (!questionEditViewModel.advancedScoring.value) {
            questionDto.baseScore = 0.0
            questionDto.maxScore = when (questionDto.type!!) {
                QuestionDto.QuestionType.SINGLE_ANSWER -> {
                    1.0
                }
                QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                    questionDto.answers!!.sumOf { max(it.chosenScore, it.notChosenScore) }
                }
                QuestionDto.QuestionType.TEXT_FIELD,
                QuestionDto.QuestionType.NUMERIC_FIELD -> {
                    1.0
                }
            }
        }

        if (find >= 0) {
            questionDto.index = questions[find].index
            questions[find] = questionDto
        } else {
            questionDto.index = questions.size
            questions.add(questionDto)
        }

    }
}


@Immutable
data class TimeLimit(val hours: Int, val minutes: Int, val seconds: Int) {
    companion object {
        fun zero(): TimeLimit {
            return TimeLimit(0, 0, 0)
        }

        fun fromSeconds(seconds: Int): TimeLimit {
            if (seconds < 0) return zero()
            val s = seconds % 60
            val m = seconds / 60 % 60
            val h = seconds / 3600
            return TimeLimit(h, m, s)
        }
    }

    fun toProperTime(): TimeLimit {
        var seconds = 0
        var minutes = 0
        var hours = 0
        if (this.seconds > 59) {
            seconds += this.seconds % 60
            minutes += this.seconds / 60
        } else {
            seconds += this.seconds
        }
        if (this.minutes > 59) {
            minutes += this.minutes % 60
            hours += this.minutes / 60
        } else {
            minutes += this.minutes
        }
        hours += this.hours
        return TimeLimit(hours, minutes, seconds)

    }

    fun toSeconds(): Int {
        return hours * 3600 + minutes * 60 + seconds;
    }

    override fun toString(): String {
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


}