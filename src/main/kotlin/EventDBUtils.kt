package ru.mipt.npm.nica.emd

class SoftwareMap(val id_to_str: Map<Short, String>, val str_to_id: Map<String, Short>)

fun getSoftwareMap(conn: java.sql.Connection): SoftwareMap {
    val query = "SELECT * FROM software_"
    val res = conn.createStatement().executeQuery(query)
    val idToStr = HashMap<Short, String>()
    val strToId = HashMap<String, Short>()
    while (res.next()) {
        val id = res.getShort("software_id")
        val ver = res.getString("software_version")
        idToStr[id] = ver
        strToId[ver] = id
    }
    return SoftwareMap(idToStr, strToId)
}

class StorageMap(val str_to_id: Map<String, Byte>)

fun getStorageMap(conn: java.sql.Connection): StorageMap {
    val query = "SELECT * FROM storage_"
    val res = conn.createStatement().executeQuery(query)
    val strToId = HashMap<String, Byte>()
    while (res.next()) {
        val id = res.getByte("storage_id")
        val storage_name = res.getString("storage_name")
        strToId[storage_name] = id
    }
    return StorageMap(strToId)
}

