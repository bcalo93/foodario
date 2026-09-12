package com.foodario

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform