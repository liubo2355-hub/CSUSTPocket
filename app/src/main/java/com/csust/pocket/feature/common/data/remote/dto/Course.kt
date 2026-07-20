package com.csust.pocket.feature.common.data.remote.dto

import androidx.annotation.Keep

@Keep
data class Course(
    val classroom: String,
    val courseName: String,
    val teacher: String,
    val weekday: String,
    val weeks: String
)