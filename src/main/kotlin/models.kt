package com.example

import org.jetbrains.exposed.sql.Table

data class Software(
    val software_id: Short,
    val software_version: String
)

object Softwares : Table("software_") {
    val software_id = integer("software_id").primaryKey()
    val software_version = varchar("software_version", 20) // .uniqueIndex()
}

data class Storage(
    val storage_id: Short,
    val storage_name: String
)

object Storages : Table("storage_") {
    val storage_id = integer("storage_id").primaryKey()
    val storage_name = varchar("storage_name", 20)
}

data class File(
    val file_guid: Int,
    val storage_id: Short,
    val file_path: String
)

object Files : Table("file_") {
    val file_guid = integer("file_guid").primaryKey()
    val storage_id = integer("storage_id").references(Storages.storage_id)
    val file_path = varchar("file_path", 255)
}

data class Event(
    val file_guid: Int,
    val event_number: Int,
    val software_id: Short,
    val period_number: Short,
    val run_number: Short,
    val track_number: Int
)

data class EventJoined(
    val file_guid: Int,
    val event_number: Int,
    val software_version: String,
    val period_number: Short,
    val run_number: Short,
    val track_number: Int
)

object Events : Table("bmn_event") {
    val file_guid = integer("file_guid").primaryKey().references(Files.file_guid)
    val event_number = integer("event_number").primaryKey()
    val software_id = integer("software_id").references(Softwares.software_id)         // there is no 'short'...
    val period_number = integer("period_number")
    val run_number = integer("run_number")
    val track_number = integer("track_number").default(-1)
}

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

