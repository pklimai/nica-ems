package com.example

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

data class Event(
    val file_guid: Int,
    val event_number: Int,
    val software_id: Short,
    val period_number: Short,
    val run_number: Short,
    val track_number: Int
)

data class EventList(val events: List<Event>)

data class StorageRaw(val storage_name: String)

data class FileRaw(
    val storage_name: StorageRaw,
    val file_path: String
)

data class SoftwareRaw(val software_version: String)

data class EventRaw(
    val storage_name: String,
    val file_path: String,
    val event_number: Int,
    val software_version: String,
    val period_number: Short,
    val run_number: Short,
    val track_number: Int
)

data class EventRawList(val events: List<EventRaw>)

