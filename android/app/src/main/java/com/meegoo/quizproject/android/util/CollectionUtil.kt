package com.meegoo.quizproject.android.util

fun <T> Iterable<T>.symmetricDifference(other: Iterable<T>): Set<T> {
    val mine = this subtract other
    val theirs = other subtract this
    return mine union theirs
}