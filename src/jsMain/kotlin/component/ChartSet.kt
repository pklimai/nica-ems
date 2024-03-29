package ru.mipt.npm.nica.ems.component

import react.Props
import react.fc
import ru.mipt.npm.nica.ems.EMSStatistics
import ru.mipt.npm.nica.ems.chartComponent

external interface ChartSetProps : Props {
    var experimentStats: EMSStatistics?
    var period: Int?
    var sw: String?
}

var chartSet = fc<ChartSetProps> { props ->
    if (props.period != null && props.sw != null) {
        val statGraphs = props.experimentStats?.periodStats?.filter { it.periodNumber == props.period }?.firstOrNull()?.softwareStats
            ?.filter { it.swVer == props.sw }?.firstOrNull()?.graphs
        statGraphs?.forEach { graph ->
            child(chartComponent) {
                attrs {
                    statGraph = graph
                }
            }
        }
    }
}
