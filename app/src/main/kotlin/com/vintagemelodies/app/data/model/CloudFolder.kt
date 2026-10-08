package com.vintagemelodies.app.data.model

data class CloudFolder(
    val name: String,
    val songCount: Int = 0,
    val displayName: String = name.removePrefix("Songs/").removePrefix("Songs\\").ifBlank { name }
)
