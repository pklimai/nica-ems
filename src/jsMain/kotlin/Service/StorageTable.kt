package ru.mipt.npm.nica.emd

import react.Props
import react.dom.*
import react.fc
import mui.material.*
import mui.x.DataGrid

external interface SSTableProps : Props {
    var content: Array<Storage>?
    // var table: String?
}

val StorageTable = fc<SSTableProps> { props ->

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

        val storageItems = props.content //json.unsafeCast<Array<Json>>()

        val rows = mutableListOf<dynamic>()

        storageItems?.forEach { item ->
            val id = item.storage_id // .unsafeCast<Json>()
            val storage_name = item.storage_name.toString()
            rows.add(
                row(
                    id,
                    storage_name
                )
            )
        }
        if (rows.size > 0) {
            console.log(columns)
            console.log(rows)
            div() {
                Card {
                    DataGrid {
                        attrs {
                            this.columns = columns.toTypedArray()
                            this.rows = rows.toTypedArray()
                            // pageSize = 10
                            /* TODO https://mui.com/components/data-grid/pagination/
                rowsPerPageOptions = arrayOf(10, 20, 30)
                onPageSizeChange = { newPageSize: Int ->
                    this.setPageSize(newPageSize)
                }
                columnBuffer = 8 */
                        }

                    }
                }
            }

        }
    }

}