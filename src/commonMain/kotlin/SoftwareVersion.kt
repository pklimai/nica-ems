package ru.mipt.npm.nica.emd

import kotlinx.serialization.Serializable

@Serializable
class SoftwareVersion(
    val software_id: Int,
    val software_version: String
)
