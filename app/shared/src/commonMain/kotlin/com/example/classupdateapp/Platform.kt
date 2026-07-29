package com.example.classupdateapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform