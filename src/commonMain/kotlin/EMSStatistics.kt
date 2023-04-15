package ru.mipt.npm.nica.ems

import kotlinx.serialization.Serializable

typealias ExperimentName = String
typealias PeriodNumber = Int
typealias SWver = String

@Serializable
class EMSStatistics(
    val totalRecords: Long,
    val periodStats: List<PeriodStats>
)

@Serializable
class PeriodStats(
    val periodNumber: PeriodNumber,
    val periodRecords: Long? = null,  // ok to be optional?
    val softwareStats: List<SWstats>? = null
)

@Serializable
class SWstats(
    val swVer: SWver,
    val graphs: Array<StatGraph>
)

@Serializable
class StatGraph(
    val title1: String,
    val title2: String,
    val slices: Array<GraphSlice>
)

@Serializable
class GraphSlice(
    val name: String,
    val value: Int
)
