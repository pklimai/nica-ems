package ru.mipt.npm.nica.ems

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

const val CONFIG_PATH = "./ems.config.yaml"

fun readConfig(): ConfigFile? {
    val mapper = ObjectMapper(YAMLFactory()).also { it.findAndRegisterModules() }

    val config: ConfigFile
    try {
        config = mapper.readValue(File(CONFIG_PATH), ConfigFile::class.java)
    } catch (e: java.lang.Exception) {
        println(
            "Could not read config file from $CONFIG_PATH. \n" +
                    "Make sure the file is there and has proper format (if in Docker, mount as volume)"
        )
        return null
    }
    println("Done reading config from $CONFIG_PATH")
    return config
}