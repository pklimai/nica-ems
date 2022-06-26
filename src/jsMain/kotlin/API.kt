package ru.mipt.npm.nica.emd

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer

import kotlinx.browser.window

val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
}

suspend fun getConfig(): ConfigFile {
    return jsonClient.get(endpoint + CONFIG_URL)
}

suspend fun getEMD(api_url: String): String {
    return jsonClient.get(endpoint + api_url)
}

suspend fun getSoftwareVersions(): Array<SoftwareVersion> {
    // TODO check if doing .get<String>(...) changes things here
    // same for .asDynamic() - sometimes it needs it!?
    return jsonClient.get<List<SoftwareVersion>>(endpoint + SOFTWARE_URL).toTypedArray()
}

suspend fun getStorages(): Array<Storage> {
    return jsonClient.get<Array<Storage>>(endpoint + STORAGE_URL) //.asDynamic()
}
