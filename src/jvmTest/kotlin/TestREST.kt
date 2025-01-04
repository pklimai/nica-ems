package ru.mipt.npm.nica.ems

import kotlin.test.Test
import kotlin.test.assertEquals

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import com.google.gson.Gson
import kotlin.test.assertFalse


const val BASE_URL = "http://127.0.0.1:8080/event_api/v1"
const val PERIOD = 7.toShort()
const val RUN = 5050

val gson = Gson()

val event1 = gson.fromJson("""
    {
        "reference": {
            "storage_name": "data2",
            "file_path": "/tmp/file12",
            "event_number": 1
        },
        "software_version": "19.1",
        "period_number": $PERIOD,
        "run_number": $RUN,
        "parameters": {
            "track_number": 11
        }
    }
""", EventRepr::class.java)

val event1_mod = gson.fromJson("""
    {
        "reference": {
            "storage_name": "data2",
            "file_path": "/tmp/file12",
            "event_number": 1
        },
        "software_version": "19.1",
        "period_number": $PERIOD,
        "run_number": $RUN,
        "parameters": {
            "track_number": 1100
        }
    }
""", EventRepr::class.java)

val event2 = gson.fromJson("""
    {
        "reference": {
            "storage_name": "data2",
            "file_path": "/tmp/file12",
            "event_number": 2
        },
        "software_version": "19.1",
        "period_number": $PERIOD,
        "run_number": $RUN,
        "parameters": {
            "track_number": 22
        }
    }
""", EventRepr::class.java)

val event3 = gson.fromJson("""
    {
        "reference": {
            "storage_name": "data2",
            "file_path": "/tmp/file12",
            "event_number": 3
        },
        "software_version": "19.1",
        "period_number": $PERIOD,
        "run_number": $RUN,
        "parameters": {
            "track_number": 33
        }
    }
""", EventRepr::class.java)


class RestApiTest {

    @Test
    fun `Checking that unauthenticated statistics request works`() {
        runBlocking {
            val response = HttpClient().get("$BASE_URL/statistics")
            assertEquals(response.status, HttpStatusCode.OK)
            println(response.bodyAsText())
            val totalRecords = Json.decodeFromString<EMSStatistics>(response.bodyAsText()).totalRecords
            assert(totalRecords >= 0)
        }
    }

    @Test
    fun `Checking that unauthenticated config request works and passwords are not exposed`() {
        runBlocking {
            val response = HttpClient(CIO).get("$BASE_URL/config")
            assertEquals(response.status, HttpStatusCode.OK)
            println(response.bodyAsText())
            val config = Json.decodeFromString<ConfigFile>(response.bodyAsText())
            assert(config.event_db.password == "")
            assert(config.condition_db?.password.isNullOrEmpty())
            assert(config.keycloak_auth?.client_secret.isNullOrEmpty())
        }
    }

    @Test
    fun `Checking that unauthenticated request for events returns 401`() {
        runBlocking {
            val response = HttpClient(CIO).get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN")
            assertEquals(response.status, HttpStatusCode.Unauthorized)
        }
    }

    @Test
    fun `Checking that unauthenticated request for software returns 401`() {
        runBlocking {
            val response = HttpClient(CIO).get("$BASE_URL/software")
            assertEquals(response.status, HttpStatusCode.Unauthorized)
        }
    }

    private fun authenticatedClient() =
        HttpClient(CIO){
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = USER, password = PASS)
                    }
                    sendWithoutRequest { request -> true }
                }
            }
            install(ContentNegotiation) {
                json()
            }
        }


    @Test
    fun `Checking if software record is present`() {
        runBlocking {
            val response = authenticatedClient().get("$BASE_URL/software")
            assertEquals(response.status, HttpStatusCode.OK)
            println(response.bodyAsText())
            val sw = response.body<Array<SoftwareVersion>>()
            assert(sw.any { it.software_version == event1.software_version })
        }
    }

    @Test
    fun `Checking if storage record is present`() {
        runBlocking {
            val response = authenticatedClient().get("$BASE_URL/storage")
            assertEquals(response.status, HttpStatusCode.OK)
            println(response.bodyAsText())
            val storages = response.body<Array<Storage>>()
            assert(storages.any { it.storage_name == event1.reference.storage_name })
        }
    }

    @Test
    fun `Long API consistency test`() {
        runBlocking {

            var response: HttpResponse

            println("************************************************************")
            println("Deleting events 1,2,3 (if present in database)")
            listOf(event1, event2, event3).forEach { event ->
                response = authenticatedClient().delete("$BASE_URL/event") {
                    contentType(ContentType.Application.Json)
                    setBody(gson.toJson(listOf(event)))
                }
                println(response.status)
                println(response.bodyAsText())
            }

            println("************************************************************")
            println("Counting events")
            response = authenticatedClient().get("$BASE_URL/count?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            val eventsCount1 = gson.fromJson(response.bodyAsText(), EventCountRepr::class.java)
            println("Initially we had ${eventsCount1.count} events")

            println("************************************************************")
            println("Creating events 1, 2")
            response = authenticatedClient().post("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event2, event1)))
            }
            println(response.status)
            println(response.bodyAsText())

            println("************************************************************")
            println("Counting events")
            response = authenticatedClient().get("$BASE_URL/count?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            val eventsCount2 = gson.fromJson(response.bodyAsText(), EventCountRepr::class.java)
            println("After creation we have ${eventsCount2.count} events")
            assertEquals(eventsCount1.count + 2, eventsCount2.count)

            println("************************************************************")
            println("Checking that events 1,2 are in catalogue and event 3 is not")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            var eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assert(event1 in eventsArray.events)
            assert(event2 in eventsArray.events)
            assertFalse(event3 in eventsArray.events)

            println("************************************************************")
            println("Creating event 3")
            response = authenticatedClient().post("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event3)))
            }
            assertEquals(response.status, HttpStatusCode.OK)
            println(response.status)
            println(response.bodyAsText())

            println("************************************************************")
            println("Checking that event 3 is now in catalogue")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assert(event3 in eventsArray.events)

            println("************************************************************")
            println("Checking that event 3 can not be written again with POST")
            response = authenticatedClient().post("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event3)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.Conflict) // 409

            println("************************************************************")
            println("Checking that event 3 can be written again with PUT")
            response = authenticatedClient().put("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event3)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.OK)

            println("************************************************************")
            println("Checking that event 3 is still in the catalogue")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assert(event3 in eventsArray.events)

            println("************************************************************")
            println("Modifying event 1 parameter (with POST, should fail)")
            response = authenticatedClient().post("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1_mod)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.Conflict) // 409

            println("************************************************************")
            println("Modifying event 1 parameter (with PUT)")
            response = authenticatedClient().put("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1_mod)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.OK)

            println("************************************************************")
            println("Checking that original event 1 is not there, but modified is")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assert(event1_mod in eventsArray.events)
            assertFalse(event1 in eventsArray.events)

            println("************************************************************")
            println("Trying to delete event 1 both original and modified (should fail)")
            response = authenticatedClient().delete("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1, event1_mod)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.NotFound)

            println("************************************************************")
            println("Checking (again) that original event 1 is not there, but modified is")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assert(event1_mod in eventsArray.events)
            assertFalse(event1 in eventsArray.events)

            println("************************************************************")
            println("Delete modified event 1")
            response = authenticatedClient().delete("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1_mod)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.OK)

            println("************************************************************")
            println("Checking that event 1 was deleted")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assertFalse(event1_mod in eventsArray.events)
            assertFalse(event1 in eventsArray.events)

            println("************************************************************")
            println("Creating again events 1,2 with POST (should fail)")
            response = authenticatedClient().post("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1, event2)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.Conflict) // 409

            println("************************************************************")
            println("Creating again events 1,2 with PUT (should work)")
            response = authenticatedClient().put("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1, event2)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.OK)

            println("************************************************************")
            println("Checking that events 1,2 are there")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assert(event1 in eventsArray.events)
            assert(event2 in eventsArray.events)

            println("************************************************************")
            println("Modifying event number for event 1 (should be treated as different event)")
            var event1_mod_event_num = gson.fromJson("""
                {
                    "reference": {
                        "storage_name": "data2",
                        "file_path": "/tmp/file12",
                        "event_number": 1000
                    },
                    "software_version": "19.1",
                    "period_number": $PERIOD,
                    "run_number": $RUN,
                    "parameters": {
                        "track_number": 11
                    }
                }
            """.trimIndent(), EventRepr::class.java)
            response = authenticatedClient().put("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1_mod_event_num)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.OK)

            println("************************************************************")
            println("Checking that event 1 both original and modified are there")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assert(event1 in eventsArray.events)
            assert(event1_mod_event_num in eventsArray.events)

            println("************************************************************")
            println("Doing filter based on track_number")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN&track_number=11") {
                contentType(ContentType.Application.Json)
            }
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            println(response.status)
            println(response.bodyAsText())
            assert(event1 in eventsArray.events)
            assert(event1_mod_event_num in eventsArray.events)
            assertFalse(event2 in eventsArray.events)
            assertFalse(event3 in eventsArray.events)

            println("************************************************************")
            println("Doing incorrect filtering expression")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN&track_number=|") {
                contentType(ContentType.Application.Json)
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.BadRequest)

            println("************************************************************")
            println("Doing filtering with unknown parameter")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN&unknown_param=90") {
                contentType(ContentType.Application.Json)
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.BadRequest)

            println("************************************************************")
            println("Delete events")
            response = authenticatedClient().delete("$BASE_URL/event") {
                contentType(ContentType.Application.Json)
                setBody(gson.toJson(listOf(event1, event1_mod_event_num, event2, event3)))
            }
            println(response.status)
            println(response.bodyAsText())
            assertEquals(response.status, HttpStatusCode.OK)

            println("************************************************************")
            println("Check that events were deleted")
            response = authenticatedClient().get("$BASE_URL/event?period_number=$PERIOD&run_number=$RUN") {
                contentType(ContentType.Application.Json)
            }
            println(response.status)
            println(response.bodyAsText())
            eventsArray = gson.fromJson(response.bodyAsText(), EventListRepr::class.java)
            assertFalse(event1 in eventsArray.events)
            assertFalse(event1_mod_event_num in eventsArray.events)
            assertFalse(event2 in eventsArray.events)
            assertFalse(event3 in eventsArray.events)

        }
    }

}



