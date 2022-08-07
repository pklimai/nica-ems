package ru.mipt.npm.nica.emd.component

import react.Props
import react.fc
import ru.mipt.npm.nica.emd.ExperimentStatistics
import ru.mipt.npm.nica.emd.chartComponent

external interface ChartSetProps : Props {
    var experimentStats: ExperimentStatistics?
    var period: Int
    var sw: String
}

var chartSet = fc<ChartSetProps> { props ->
    child(chartComponent) {
        attrs {
            statGraph = props.experimentStats?.periodStats?.get(props.period)?.softwareStats?.get(props.sw)?.graphs?.get(0)
        }
    }
}