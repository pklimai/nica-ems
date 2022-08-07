package ru.mipt.npm.nica.emd

import kotlinx.serialization.Serializable

typealias ExperimentName = String
typealias PeriodNumber = Int
typealias SWver = String

@Serializable
class EMSStatistics(
    val experimentStatistics: Map<ExperimentName, ExperimentStatistics>
)

@Serializable
class ExperimentStatistics(
    val totalRecords: Long,
    val periodStats: Map<PeriodNumber, PeriodStats>
)

@Serializable
class PeriodStats(
    val periodRecords: Long? = null,  // ok to be optional?
    val softwareStats: Map<SWver, SWstats>? = null
)

@Serializable
class SWstats(
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
    val yValue: Int,
    val name: String
)
