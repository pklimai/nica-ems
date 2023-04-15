package ru.mipt.npm.nica.ems

data class UserRoles(
    val isReader: Boolean,
    val isWriter: Boolean,
    val isAdmin: Boolean
)
