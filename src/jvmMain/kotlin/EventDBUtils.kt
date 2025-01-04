package ru.mipt.npm.nica.ems

import io.ktor.server.application.*
import io.ktor.server.auth.*
import kotlinx.html.BODY
import kotlinx.html.p
import org.postgresql.util.PSQLException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

fun newEMDConnection(config: ConfigFile, context: ApplicationCall, forStatsGetting: Boolean = false): Connection? {
    val urlEventDB =
        "jdbc:postgresql://${config.event_db.host}:${config.event_db.port}/${config.event_db.db_name}"
    // val connEMD = DriverManager.getConnection(urlEventDB, config.event_db.user, config.event_db.password)
    if (config.database_auth == true && !forStatsGetting) {
        val user = context.principal<UserIdPwPrincipal>()!!.name
        val pass = context.principal<UserIdPwPrincipal>()!!.pw
        try {
            val connection = DriverManager.getConnection(urlEventDB, user, pass)
            return connection
        } catch (_: PSQLException) {
            return null
        }
    } else {
        // KeyCloak or no auth at all, or we are collecting stats for homepage
        return DriverManager.getConnection(urlEventDB, config.event_db.user, config.event_db.password)
    }
}

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

fun queryEMD(
    parameterBundle: ParameterBundle,
    page: PageConfig,
    connCondition: Connection?,
    connEMD: Connection,
    body: BODY?,
    defaultLimit: Int? = null,
    countOnly: Boolean = false
): ResultSet? {
    with(parameterBundle) {
        val et = page.db_table_name
        // TODO: Check how joins affect the performance. Consider doing DIY joins?
        var query =
            """SELECT ${if (countOnly) "count(1)" else "*"} FROM $et 
                  INNER JOIN software_ ON $et.software_id = software_.software_id
                  INNER JOIN file_ ON $et.file_guid = file_.file_guid
                  INNER JOIN storage_ ON file_.storage_id = storage_.storage_id
                  """

        val filterCriteria = ArrayList<String>()
        period_number?.let { filterCriteria.add(it.generateSQLWhere()) }
        run_number?.let { filterCriteria.add(it.generateSQLWhere()) }
        software_version?.let { filterCriteria.add(it.generateSQLWhere()) }
        parametersSupplied.forEach { filterCriteria.add(it.value.generateSQLWhere()) }

        connCondition?.let {
            if (beam_particle != null || target_particle != null || energy != null) {
                val periodsRuns =
                    getRunsBy(connCondition, beam_particle, target_particle, energy)
                if (periodsRuns.isEmpty()) {
                    body?.p {
                        +"""WARNING: Empty set of (period_number, run_number) returned in
                                            pre-selection, not using it"""
                    }
                    println("WARNING: Empty set of (period_number, run_number) returned in pre-selection, not using it")
                    return null
                } else {
                    val periodsRunsJoined = periodsRuns
                        .joinToString(", ", prefix = "( ", postfix = " )")
                        { "(${it.first}, ${it.second})" }
                    body?.p { +"Preselection (period_number, run_number) returned: $periodsRunsJoined" }
                    println("Preselection (period_number, run_number) returned: $periodsRunsJoined")
                    filterCriteria.add(" (period_number, run_number) IN $periodsRunsJoined")
                }
            }
        }

        if (filterCriteria.isNotEmpty()) {
            query += " WHERE " + filterCriteria.joinToString(" AND ")
        }

        if (limit != null) {
            query += " LIMIT ${limit.stringValue}"
        } else if (defaultLimit != null) {
            query += " LIMIT $defaultLimit"
        }
        offset?.let {
            query += " OFFSET ${offset.stringValue}"
        }

        println(query)

        try {
            val res = connEMD.createStatement().executeQuery(query)
            return res
        } catch (err: PSQLException) {
            println("Error querying the database: ${err}")
            return null
        }

    }
}
