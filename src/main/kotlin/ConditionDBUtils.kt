package com.example

import java.sql.Connection

typealias PeriodRunList = List<Pair<Short, Int>>

fun getRunsBy(conn: Connection, beamParticle: String?, targetParticle: String?, energy: Float): PeriodRunList {
    val res = conn.createStatement().executeQuery("""
        SELECT period_number, run_number FROM run_ 
        WHERE beam_particle = '$beamParticle' AND target_particle = '$targetParticle'  
    """.trimIndent())   // TODO - AND energy ...
    return ArrayList<Pair<Short, Int>>().apply {
        while (res.next()) {
            add(Pair(res.getShort("period_number"), res.getInt("run_number")))
        }
    }
}
