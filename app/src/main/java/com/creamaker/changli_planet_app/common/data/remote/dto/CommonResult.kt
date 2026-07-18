package com.creamaker.changli_planet_app.common.data.remote.dto

data class CommonResult<T>(
    val data: T?,
    val code: String,
    val msg: String,
)
