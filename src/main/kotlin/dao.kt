package com.example

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Closeable

interface DAOEventInterface : Closeable {
    fun init()
    fun createEvent(
        file_guid: Int,
        event_number: Int,
        software_id: Short,
        period_number: Int,
        run_number: Int,
        track_number: Int
    )

//    fun updateEvent(file_ptr: Int, event_num: Int, period: Int, run: Int, software_id: Short, all_tracks: Int)
//    fun deleteEvent(file_ptr: Int, event_num: Int)

    fun getEvent(file_ptr: Int, event_num: Int): Event?
    fun getAllEvents(): List<Event>
}

class EventDAO(val db: Database) : DAOEventInterface {
    override fun init() = transaction(db) {
        SchemaUtils.create(Events)
    }

    override fun createEvent(
        file_guid: Int,
        event_number: Int,
        software_id: Short,
        period_number: Int,
        run_number: Int,
        track_number: Int
    ) =
        transaction(db) {
            Events.insert {
                it[Events.file_guid] = file_guid
                it[Events.event_number] = event_number
                it[Events.software_id] = software_id.toInt()
                it[Events.period_number] = period_number
                it[Events.run_number] = run_number
                it[Events.track_number] = track_number
            }
            Unit
        }

//    override fun updateEvent(file_ptr: Int, event_num: Int, period: Int, run: Int, software_id: Short, all_tracks: Int) {
//        TODO("Not yet implemented")
//    }
//    override fun deleteEvent(file_ptr: Int, event_num: Int) {
//        TODO("Not yet implemented")
//    }

    override fun getEvent(file_ptr: Int, event_num: Int): Event? = transaction(db) {
        Events.select { (Events.file_guid eq file_ptr) and (Events.event_number eq event_num) }.map {
            Event(
                it[Events.file_guid],
                it[Events.event_number],
                it[Events.software_id].toShort(),
                it[Events.period_number].toShort(),
                it[Events.run_number].toShort(),
                it[Events.track_number]
            )
        }.singleOrNull()
    }

    override fun getAllEvents(): List<Event> =
        transaction(db) {
            Events.selectAll().map {
                Event(
                    it[Events.file_guid],
                    it[Events.event_number],
                    it[Events.software_id].toShort(),
                    it[Events.period_number].toShort(),
                    it[Events.run_number].toShort(),
                    it[Events.track_number]
                )
            }
        }

    fun getAllEventsJoined(): List<EventJoined> =
        transaction(db) {
            (Events innerJoin Softwares).selectAll().map {
                EventJoined(
                    it[Events.file_guid],
                    it[Events.event_number],
                    it[Softwares.software_version],
                    it[Events.period_number].toShort(),
                    it[Events.run_number].toShort(),
                    it[Events.track_number]
                )
            }
        }

    fun searchEvents(period: Int, run: Int): List<Event> = transaction(db) {
        Events.select { (Events.period_number eq period) and (Events.run_number eq run) }.map {
            Event(
                it[Events.file_guid],
                it[Events.event_number],
                it[Events.software_id].toShort(),
                it[Events.period_number].toShort(),
                it[Events.run_number].toShort(),
                it[Events.track_number]
            )
        }
    }

    override fun close() {}

}
