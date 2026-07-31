package com.cnctech.process.data.repository

sealed class AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>()
    data class Err(val message: String) : AppResult<Nothing>()
}

inline fun <T> AppResult<T>.getOrElse(onErr: (String) -> T): T = when (this) {
    is AppResult.Ok -> value
    is AppResult.Err -> onErr(message)
}
