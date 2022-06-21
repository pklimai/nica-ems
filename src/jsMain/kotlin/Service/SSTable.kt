package ru.mipt.npm.nica.emd

import csstype.pct
import kotlinext.js.jso
import react.Props
import react.dom.*
import react.fc
import kotlin.js.Json
import mui.material.*
import mui.x.DataGrid


external interface SSTableProps : Props {
    var content: String?
    var pageConfig: PageConfig
    var table: String?
}

val SSTable = fc<SSTableProps> { props ->
    console.log(props.content)
    if (props.content != null) {
        if(props.table == "Storage"){
            fun column(field: String, key: String /* TODO check */, headerName: String): dynamic {
                val r: dynamic = object {}
                r["field"] = field
                r["key"] = key
                r["headerName"] = headerName
                return r
            }

            val columns = mutableListOf(
                column("storage_name", "storage_name", "Storage Name"),
            )

            fun row(
                id: Int,
                storage_name: String
            ): dynamic {
                val r: dynamic = object {}
                r["id"] = id
                r["storage_name"] = storage_name
                return r
            }

            //console.log(props.content)
            val json = JSON.parse<Json>(props.content!!)
            //console.log(json["events"])
            val events = json["events"].unsafeCast<Array<Json>>()

            val rows = mutableListOf<dynamic>()

            var id: Int = 1
            events.forEach { event ->
                // console.log(event)
                val ref = event["reference"].unsafeCast<Json>()
                val storage_name = ref["storage_name"]

                rows.add(
                    row(
                        id++,
                        storage_name.toString(),
                    )
                )

            }

            div("div-emd-table-card") {
                Card {
                    DataGrid {
                        attrs {
                            this.columns = columns.toTypedArray()
                            this.rows = rows.toTypedArray()
                            pageSize = 10
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
        if(props.table == "Software"){ //Software
            fun column(field: String, key: String /* TODO check */, headerName: String): dynamic {
                val r: dynamic = object {}
                r["field"] = field
                r["key"] = key
                r["headerName"] = headerName
                return r
            }

            val columns = mutableListOf(
                column("software_version", "software_version", "Software Version"),
            )

            fun row(
                id: Int,
                software_version: String
            ): dynamic {
                val r: dynamic = object {}
                r["id"] = id
                r["software_version"] = software_version
                return r
            }

            //console.log(props.content)
            val json = JSON.parse<Json>(props.content!!)
            //console.log(json["events"])
            val events = json["events"].unsafeCast<Array<Json>>()

            val rows = mutableListOf<dynamic>()

            var id: Int = 1
            events.forEach { event ->
                // console.log(event)
                val ref = event["reference"].unsafeCast<Json>()
                val software_version = ref["software_version"]

                rows.add(
                    row(
                        id++,
                        software_version.toString(),
                    )
                )

            }

            div("div-emd-table-card") {
                Card {
                    DataGrid {
                        attrs {
                            this.columns = columns.toTypedArray()
                            this.rows = rows.toTypedArray()
                            pageSize = 10
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
    } else {
        Card {
            h3 {
                + "No data"
            }
        }
    }
}