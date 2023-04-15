package ru.mipt.npm.nica.ems

data class EventRepr(
    val reference: Reference,
    val software_version: String, // ?
    val period_number: Short,
    val run_number: Int,
    val parameters: Map<String, Any>  // works with Int for Int...
)

data class Reference(
    val storage_name: String,
    val file_path: String,
    val event_number: Int
)
