package com.example.myapplication

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val groupTitle: String = ""
)
