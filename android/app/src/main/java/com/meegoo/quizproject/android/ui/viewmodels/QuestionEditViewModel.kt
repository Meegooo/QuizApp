package com.meegoo.quizproject.android.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.meegoo.quizproject.android.data.dto.QuestionDto
import java.util.*


class QuestionEditViewModel : ViewModel() {
    var id = mutableStateOf<UUID>(UUID.randomUUID())
    var question = mutableStateOf("")
    var questionType = mutableStateOf<QuestionDto.QuestionType?>(null)
    var weight = mutableStateOf("1.0")
    var answers = mutableStateListOf<QuestionDto.AnswerDto>()
    var baseScore = mutableStateOf("0.0")
    var maxScore = mutableStateOf("0.0")
    var numericPrecision = mutableStateOf(4)
    var trimPadding = mutableStateOf(false)
    var ignoreCase = mutableStateOf(false)

    var previewingQuestion = mutableStateOf(false)
    var showingMarkdownHelp = mutableStateOf(false)
    var advancedScoring = mutableStateOf(false)


    fun fromQuestionDto(questionDto: QuestionDto) {
        this.id.value = questionDto.id ?: UUID.randomUUID()
        this.question.value = questionDto.question ?: ""
        this.questionType.value = questionDto.type
        this.weight.value = questionDto.weight?.toString() ?: "1.0"
        this.answers.clear()
        this.answers.addAll(questionDto.answers ?: emptyList())
        this.baseScore.value = questionDto.baseScore?.toString() ?: "0.0"
        this.maxScore.value = questionDto.maxScore?.toString() ?: "0.0"
        this.numericPrecision.value = questionDto.numericPrecision ?: 4
        this.trimPadding.value = questionDto.trimPadding ?: false
        this.ignoreCase.value = questionDto.trimPadding ?: false

        if (this.baseScore.value.toDouble() != 0.0) {
            advancedScoring.value = true
        }
        when (questionType.value) {
            QuestionDto.QuestionType.SINGLE_ANSWER -> {
                if (this.maxScore.value.toDouble() != 1.0) {
                    advancedScoring.value = true
                } else if (answers.count { it.chosenScore == 1.0 } != 1 ||
                    answers.sumOf { it.chosenScore } != 1.0 ||
                    answers.any { it.notChosenScore != 0.0 }) {
                    advancedScoring.value = true
                }
            }
            QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                if (this.maxScore.value.toDouble() != this.answers.size.toDouble()) {
                    advancedScoring.value = true
                } else if (!answers.all {
                        it.chosenScore == 0.0 && it.notChosenScore == 1.0 ||
                                it.chosenScore == 1.0 && it.notChosenScore == 0.0
                    }) {
                    advancedScoring.value = true
                }
            }

            QuestionDto.QuestionType.NUMERIC_FIELD,
            QuestionDto.QuestionType.TEXT_FIELD -> {
                if (this.maxScore.value.toDouble() != 1.0) {
                    advancedScoring.value = true
                } else if (!this.answers.all { it.chosenScore == 1.0 && it.notChosenScore == 0.0 }) {
                    advancedScoring.value = true
                }
            }
        }

    }


    fun toQuestionDto(): QuestionDto {
        return QuestionDto(
            id.value,
            question.value,
            questionType.value,
            weight.value.toDoubleOrNull() ?: 0.0,
            ArrayList(answers),
            0,
            baseScore.value.toDoubleOrNull() ?: 0.0,
            maxScore.value.toDoubleOrNull() ?: 0.0,
            ignoreCase.value,
            numericPrecision.value,
            trimPadding.value
        )
    }

}
