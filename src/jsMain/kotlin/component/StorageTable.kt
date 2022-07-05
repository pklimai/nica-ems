package ru.mipt.npm.nica.emd

import react.Props
import react.dom.*
import react.fc
import mui.material.*
import mui.x.DataGrid

external interface StorageTableProps : Props {
    var content: Array<Storage>?
}

val StorageTable = fc<StorageTableProps> { props ->

    console.log(props.content)
    //val json = JSON.parse<Json>(props.content ?: "{}")
    //console.log(json)

    if (props.content.isNullOrEmpty()) {
        Card {
            h3 {
                +"No data"
            }
        }
    } else {
        val columns = mutableListOf(
            column("storage_id", "storage_id", "Storage ID", 1),
            column("storage_name", "storage_name", "Storage Name", 1),
        )

        fun row(
            id: Int,
            storage_name: String
        ): dynamic {
            val r: dynamic = object {}
            r["id"] = id
            r["storage_id"] = id
            r["storage_name"] = storage_name
            return r
        }

        val rows = mutableListOf<dynamic>()

        props.content?.forEach { item ->
            val id = item.storage_id
            val storage_name = item.storage_name
            rows.add(row(id, storage_name))
        }
        //if (rows.size > 0) {
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
        //}
    }
}
