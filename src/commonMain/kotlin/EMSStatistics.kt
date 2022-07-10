package ru.mipt.npm.nica.emd

import kotlinx.serialization.Serializable

typealias ExperimentName = String
typealias PeriodNumber = Int

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
)
