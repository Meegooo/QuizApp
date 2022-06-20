package com.meegoo.quizproject.server.data

object JacksonView {
    interface Overview
    interface Read : Overview
    interface Answers : Read
    interface Write : Read
}

enum class JacksonViewEnum {
    OVERVIEW, READ, ANSWER, WRITE;
}