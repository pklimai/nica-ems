package ru.mipt.npm.nica.ems

import react.Props
import react.dom.*
import react.fc
import mui.material.*
import mui.x.DataGrid

external interface StorageTableProps : Props {
    var content: Array<Storage>?
}

val storageTable = fc<StorageTableProps> { props ->

    if (props.content.isNullOrEmpty()) {
        Card {
            h3 {
                +"No data"
            }
        }
    } else {

        val columns = mutableListOf(
            column("storage_id", "storage_id", "Storage ID", 1),
            column("storage_name", "storage_name", "Storage Name", 2),
        )

        fun row(id: Int, storage_name: String): dynamic {
            val r: dynamic = object {}
            r["id"] = id
            r["storage_id"] = id
            r["storage_name"] = storage_name
            return r
        }

        val rows = buildList<dynamic> {
            props.content?.forEach { item ->
                this.add(row(item.storage_id, item.storage_name))
            }
        }

        div("dictionary_table_page") {
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
}
