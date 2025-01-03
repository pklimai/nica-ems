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

data class EventReprForDelete(
    val reference: Reference,
    val software_version: String? = "",
    val period_number: Short? = 0,
    val run_number: Int? = 0,
    val parameters: Map<String, Any>?
)

fun EventReprForDelete.str(): String =
    "(${this.reference.storage_name}, ${this.reference.file_path}, ${this.reference.event_number})"

data class EventListRepr(
    val events: Array<EventRepr>
)
