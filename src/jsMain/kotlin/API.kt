package ru.mipt.npm.nica.emd

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window

val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

val jsonClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

val stringClient = HttpClient { }

suspend fun getConfig(): ConfigFile {
    return jsonClient.get(endpoint + CONFIG_URL).body()
}

suspend fun getEMD(api_url: String): String {
    return stringClient.get(endpoint + api_url).body()
}

suspend fun getSoftwareVersions(): Array<SoftwareVersion> {
    return jsonClient.get(endpoint + SOFTWARE_URL).body()
}

suspend fun getStorages(): Array<Storage> {
    return jsonClient.get(endpoint + STORAGE_URL).body()
}
