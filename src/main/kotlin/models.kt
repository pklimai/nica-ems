package com.example

/*
data class Software(
    val software_id: Short,
    val software_version: String
)

data class Storage(
    val storage_id: Short,
    val storage_name: String
)

data class File(
    val file_guid: Int,
    val storage_id: Short,
    val file_path: String
)
*/

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
