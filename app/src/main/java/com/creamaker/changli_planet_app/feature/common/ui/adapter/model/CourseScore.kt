package com.creamaker.changli_planet_app.feature.common.ui.adapter.model

import androidx.annotation.Keep

@Keep
data class CourseScore(
    val id: String,
    val semester: String,
    val name: String,
    val score: Int,
    val scoreText: String,
    val credit: Double,
    val earnedCredit: Double,
    val totalHours: String,
    val studyType: String,
    val courseNature: String,
    val courseType: String,
    val assessmentMethod: String,
    val examNature: String,
    val pscjUrl: String?
)
