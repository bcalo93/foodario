package com.foodario

internal suspend fun catchError(block: suspend () -> Unit): Throwable? = try {
    block()
    null
} catch (t: Throwable) {
    t
}
