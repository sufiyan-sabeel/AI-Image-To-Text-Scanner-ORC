package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class SlideItem(
    val slideNumber: Int,
    val title: String,
    val subtitle: String = "",
    val bulletPoints: List<String> = emptyList(),
    val keyTakeaway: String = ""
)
