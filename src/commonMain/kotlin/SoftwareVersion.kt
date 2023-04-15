package ru.mipt.npm.nica.ems

import kotlinx.serialization.Serializable

@Serializable
class SoftwareVersion(
    val software_id: Int,
    val software_version: String
)
