package ru.mipt.npm.nica.emd

import react.Props
import react.dom.*
import react.fc
import mui.material.*
import mui.x.DataGrid

external interface SoftwareTableProps : Props {
    var content: Array<SoftwareVersion>?
}

val softwareTable = fc<SoftwareTableProps> { props ->

    val columns = mutableListOf(
        column("software_id", "software_id", "Software ID", 1),
        column("software_version", "software_version", "Software Version", 3),
    )

    fun row(
        id: Int,
        software_version: String
    ): dynamic {
        val r: dynamic = object {}
        r["id"] = id
        r["software_id"] = id
        r["software_version"] = software_version
        return r
    }

    val rows = buildList<dynamic> {
        props.content?.forEach { item ->
            this.add(row(item.software_id, item.software_version))
        }
    }

    div("new_table_page") {
        div("new_table_page__dicttable") {
            div("div-emd-table-card") {
                Card {
                    DataGrid {
                        attrs {
                            this.columns = columns.toTypedArray()
                            this.rows = rows.toTypedArray()
                            pageSize = 10
                        }
                    }
                }
            }
        }
    }
}