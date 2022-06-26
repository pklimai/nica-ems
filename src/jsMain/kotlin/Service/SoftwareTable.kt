package ru.mipt.npm.nica.emd.Service

//if(props.table == "Software"){ //Software
//
//    val columns = mutableListOf(
//        column("software_version", "software_version", "Software Version"),
//    )
//
//    fun row(
//        id: Int,
//        software_version: String
//    ): dynamic {
//        val r: dynamic = object {}
//        r["id"] = id
//        r["software_version"] = software_version
//        return r
//    }
//
//    //console.log(props.content)
//    val json = JSON.parse<Json>(props.content!!)
//    //console.log(json["events"])
//    val events = json["events"].unsafeCast<Array<Json>>()
//
//    val rows = mutableListOf<dynamic>()
//
//    var id: Int = 1
//    events.forEach { event ->
//        // console.log(event)
//        val ref = event["reference"].unsafeCast<Json>()
//        val software_version = ref["software_version"]
//
//        rows.add(
//            row(
//                id++,
//                software_version.toString(),
//            )
//        )
//
//    }
//
//    div("div-emd-table-card") {
//        Card {
//            DataGrid {
//                attrs {
//                    this.columns = columns.toTypedArray()
//                    this.rows = rows.toTypedArray()
//                    pageSize = 10
//                    /* TODO https://mui.com/components/data-grid/pagination/
//                rowsPerPageOptions = arrayOf(10, 20, 30)
//                onPageSizeChange = { newPageSize: Int ->
//                    this.setPageSize(newPageSize)
//                }
//                columnBuffer = 8 */
//                }
//
//            }
//        }
//    }
//}
//}