package com.meegoo.quizproject.android.data.dto


enum class LoadLevel(val level: Int) {
    MISSING(-1), OVERVIEW(0), READ(1), ANSWER(2), WRITE(3),
}