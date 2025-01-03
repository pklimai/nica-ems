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


const val BASE_URL = "http://127.0.0.1:8080/event_api/v1"
const val PERIOD = 7.toShort()
const val RUN = 5050

val event1 = EventRepr(
    Reference("data2", "/tmp/file12", 1),
    "19.1",
    PERIOD,
    RUN,
    mapOf("track_number" to 11)
)

val event1_mod = EventRepr(
    Reference("data2", "/tmp/file12", 1),
    "19.1",
    PERIOD,
    RUN,
    mapOf("track_number" to 1100)
)

val event2 = EventRepr(
    Reference("data2", "/tmp/file12", 2),
    "19.1",
    PERIOD,
    RUN,
    mapOf("track_number" to 22)
)

val event3 = EventRepr(
    Reference("data2", "/tmp/file12", 3),
    "19.1",
    PERIOD,
    RUN,
    mapOf("track_number" to 33)
)


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

}



