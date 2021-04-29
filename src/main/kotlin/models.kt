package com.example

import org.jetbrains.exposed.sql.Table

data class Event (
    val file_ptr: Int,
    val event_num: Int,
    val period: Int,
    val run: Int,
    val sw_ver: Short,
    val all_tracks: Int
)

object Events : Table() {
    val file_ptr = integer("file_ptr").primaryKey()
    val event_num = integer("event_num").primaryKey()
    val period = integer("period")
    val run = integer("run")
    val sw_ver = integer("sw_ver")         // there is no 'short'...
    val all_tracks = integer("all_tracks")
}
