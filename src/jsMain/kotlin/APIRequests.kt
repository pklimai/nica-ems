package ru.mipt.npm.nica.emd

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.engine.js.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window
import ru.mipt.npm.nica.emd.utility.EMSConflictException
import ru.mipt.npm.nica.emd.utility.EMSServerError
import ru.mipt.npm.nica.emd.utility.EMSUnauthException

val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

suspend fun getConfig(): ConfigFile {
    val jsonClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    val res = jsonClient.get(endpoint + CONFIG_URL).body<ConfigFile>()
    jsonClient.close()
    return res
}

suspend fun getEMD(api_url: String, config: ConfigFile?, username: String, password: String): String {
    val stringClient = HttpClient(Js) {
        if (config?.authRequired() == true) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = username, password = password)
                    }
                    // enable sending credentials in the initial request without waiting for a 401 (Unauthorized) response:
                    sendWithoutRequest { request -> true }
                }
            }
        }
    }
    val httpResp = stringClient.get(endpoint + api_url)
    stringClient.close()
    if (httpResp.status == HttpStatusCode.Unauthorized) {
        // Should not be reachable unless something improbable happened - because auth check must be done earlier
        // but can get here in corner cases such as password on server was changed, etc.
        console.log("Got HttpStatusCode Unauthorized!")
        throw EMSUnauthException()
    }
    val res = httpResp.bodyAsText()
    console.log(res)
    return res
}

fun jsonClientWithOptionalAuth(config: ConfigFile?, username: String, password: String): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json()
        }
        if (config?.authRequired() == true) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = username, password = password)
                    }
                    sendWithoutRequest { request -> true }
                }
            }
        }
    }

suspend fun getSoftwareVersions(config: ConfigFile?, username: String, password: String): Array<SoftwareVersion> {
    val jsonClient = jsonClientWithOptionalAuth(config, username, password)
    val httpResponse = jsonClient.get(endpoint + SOFTWARE_URL)
    jsonClient.close()
    if (httpResponse.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.NotFound)) {
        throw EMSUnauthException()
    }
    return httpResponse.body<Array<SoftwareVersion>>()
}

suspend fun getStorages(config: ConfigFile?, username: String, password: String): Array<Storage> {
    val jsonClient = jsonClientWithOptionalAuth(config, username, password)
    val httpResponse = jsonClient.get(endpoint + STORAGE_URL)
    jsonClient.close()
    if (httpResponse.status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.NotFound)) {
        throw EMSUnauthException()
    }
    return httpResponse.body<Array<Storage>>()
}

suspend fun postSoftwareVersion(swVer: String, config: ConfigFile?, username: String, password: String): Unit {
    val jsonClient = jsonClientWithOptionalAuth(config, username, password)
    val res = jsonClient.post(endpoint + SOFTWARE_URL) {
        setBody(SoftwareVersion(0 /* ignored */, swVer))
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.ContentType, "application/json")
        }
    }
    when (res.status) {
        HttpStatusCode.Unauthorized -> throw EMSUnauthException()
        HttpStatusCode.Conflict -> throw EMSConflictException()
        HttpStatusCode.InternalServerError -> throw EMSServerError()
    }
}

suspend fun postStorage(storage: String, config: ConfigFile?, username: String, password: String): Unit {
    val jsonClient = jsonClientWithOptionalAuth(config, username, password)
    val res = jsonClient.post(endpoint + STORAGE_URL) {
        setBody(Storage(0 /* ignored */, storage))
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.ContentType, "application/json")
        }
    }
    when (res.status) {
        HttpStatusCode.Unauthorized -> throw EMSUnauthException()
        HttpStatusCode.Conflict -> throw EMSConflictException()
        HttpStatusCode.InternalServerError -> throw EMSServerError()
    }
}

suspend fun getStats(): EMSStatistics {
    return EMSStatistics(
        mapOf(
            "BM@N" to ExperimentStatistics(
                totalRecords = 50000,
                periodStats = listOf(
                    PeriodStats(1),
                    PeriodStats(2),
                    PeriodStats(3),
                    PeriodStats(4),
                    PeriodStats(5),
                    PeriodStats(6),
                    PeriodStats(7),
                    PeriodStats(
                        8,
                        10000,
                        listOf(
                            SWstats(
                                "19.1",
                                arrayOf(
                                    StatGraph(
                                        "My Stat Graph ONE",
                                        "",
                                        arrayOf(
                                            GraphSlice("Fe", 1),
                                            GraphSlice("Cu", 2),
                                            GraphSlice("Be", 3)
                                        )
                                    )
                                )
                            ),
                            SWstats(
                                "20.1",
                                arrayOf(
                                    StatGraph(
                                        "My Stat Graph TWO-1",
                                        "",
                                        arrayOf(
                                            GraphSlice("Au", 4),
                                            GraphSlice("B", 5),
                                            GraphSlice("C", 6)
                                        )
                                    ),
                                    StatGraph(
                                        "My Stat Graph TWO-2",
                                        "",
                                        arrayOf(
                                            GraphSlice("Au", 44),
                                            GraphSlice("B", 55),
                                            GraphSlice("C", 66)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            "SRC" to ExperimentStatistics(
                totalRecords = 70000,
                periodStats = listOf(
                    PeriodStats(1),
                    PeriodStats(2),
                    PeriodStats(3),
                    PeriodStats(
                        4,
                        10000,
                        listOf(
                            SWstats(
                                "19.1",
                                arrayOf(
                                    StatGraph(
                                        "My Stat Graph TWO-1",
                                        "",
                                        arrayOf(
                                            GraphSlice("Au", 4),
                                            GraphSlice("B", 5),
                                            GraphSlice("C", 6)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            "Test" to ExperimentStatistics(
                totalRecords = 90000,
                periodStats = listOf(
                    PeriodStats(1),
                    PeriodStats(
                        2,
                        10000,
                        listOf(
                            SWstats(
                                "19.1",
                                arrayOf(
                                    StatGraph(
                                        "My Stat Graph TWO-1",
                                        "",
                                        arrayOf(
                                            GraphSlice("Au", 4),
                                            GraphSlice("B", 5),
                                            GraphSlice("C", 6)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    ) // TODO use STATISTICS_URL
}
