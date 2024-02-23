package ru.mipt.npm.nica.ems

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val sub: String,
    val email_verified: Boolean,
    val name: String,
    val groups: List<String>,
    val preferred_username: String,
    val given_name: String,
    val family_name: String,
    val email: String
)
