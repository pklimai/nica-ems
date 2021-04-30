package com.example

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Closeable

interface DAOEventInterface : Closeable {
    fun init()
    fun createEvent(file_ptr: Int, event_num: Int, period: Int, run: Int, sw_ver: Short, all_tracks: Int)

//    fun updateEvent(file_ptr: Int, event_num: Int, period: Int, run: Int, sw_ver: Short, all_tracks: Int)
//    fun deleteEvent(file_ptr: Int, event_num: Int)
    fun getEvent(file_ptr: Int, event_num: Int): Event?
    fun getAllEvents(): List<Event>
}

class EventDAO(val db: Database) : DAOEventInterface {
    override fun init() = transaction(db) {
        SchemaUtils.create(Events)
    }

    override fun createEvent(file_ptr: Int, event_num: Int, period: Int, run: Int, sw_ver: Short, all_tracks: Int) =
        transaction(db) {
            Events.insert {
                it[Events.file_ptr] = file_ptr
                it[Events.event_num] = event_num
                it[Events.period] = period
                it[Events.run] = run
                it[Events.sw_ver] = sw_ver.toInt()
                it[Events.all_tracks] = all_tracks
            }
            Unit
        }

    //    override fun updateEvent(file_ptr: Int, event_num: Int, period: Int, run: Int, sw_ver: Short, all_tracks: Int) {
//        TODO("Not yet implemented")
//    }
//    override fun deleteEvent(file_ptr: Int, event_num: Int) {
//        TODO("Not yet implemented")
//    }
//
    override fun getEvent(file_ptr: Int, event_num: Int): Event? = transaction(db) {
        Events.select { (Events.file_ptr eq file_ptr) and (Events.event_num eq event_num) }.map {
            Event(
                it[Events.file_ptr],
                it[Events.event_num],
                it[Events.period],
                it[Events.run],
                it[Events.sw_ver].toShort(),
                it[Events.all_tracks]
            )
        }.singleOrNull()
    }

    override fun getAllEvents(): List<Event> =
        transaction(db) {
            Events.selectAll().map {
                Event(
                    it[Events.file_ptr],
                    it[Events.event_num],
                    it[Events.period],
                    it[Events.run],
                    it[Events.sw_ver].toShort(),
                    it[Events.all_tracks]
                )
            }
        }

    override fun close() {}

}
