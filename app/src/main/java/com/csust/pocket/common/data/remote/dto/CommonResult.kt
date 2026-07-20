package com.csust.pocket.common.data.remote.dto

data class CommonResult<T>(
    val data: T?,
    val code: String,
    val msg: String,
)
