package com.example

import java.lang.Exception
import java.sql.Connection

typealias PeriodRunList = List<Pair<Short, Int>>

fun getRunsBy(
    conn: Connection,
    beamParticle: Parameter?,
    targetParticle: Parameter?,
    energy: Parameter?
): PeriodRunList {
    if (beamParticle == null && targetParticle == null && energy == null) {
        throw Exception("Should provide some search criteria!")
    }
    var sql = "SELECT period_number, run_number FROM run_"
    val filteringConditions = ArrayList<String>()
    beamParticle?.let {
        filteringConditions.add(it.generateSQLWhere())
    }
    targetParticle?.let {
        filteringConditions.add(it.generateSQLWhere())
    }
    energy?.let {
        filteringConditions.add(it.generateSQLWhere())
    }
    sql += " WHERE " + filteringConditions.joinToString(" AND ")

    val res = conn.createStatement().executeQuery(sql)
    return ArrayList<Pair<Short, Int>>().apply {
        while (res.next()) {
            add(Pair(res.getShort("period_number"), res.getInt("run_number")))
        }
    }
}
