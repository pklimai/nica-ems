package ru.mipt.npm.nica.ems

import kotlinx.serialization.Serializable

@Serializable
class Storage(
    val storage_id: Int,
    val storage_name: String
)
