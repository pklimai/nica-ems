import csstype.pct
import csstype.px
import kotlinext.js.jso
import kotlinx.serialization.Serializable
import react.Props
import react.dom.*
import react.fc
import kotlin.js.Json
import mui.material.*
import react.ReactNode
import mui.x.DataGrid


external interface EMDTableProps : Props {
    // TODO make structured
    var content: String?
    var pageConfig: PageConfig
}

val EMDTable = fc<EMDTableProps> { props ->


    val c1: dynamic = object { }
    c1["field"] = "id"
    c1["key"] = 1
    c1["headerName"] = "ID"
    c1["width"] = 90
    val c2: dynamic = object { }
    c2["field"] = "firstName"
    c2["key"] = 2
    c2["headerName"] = "First name"
    c2["width"] = 150
    val c3: dynamic = object { }
    c3["field"] = "lastName"
    c3["key"] = 3
    c3["headerName"] = "Last name"
    c3["width"] = 150

    val columns = arrayOf(c1, c2, c3)

    fun row(id: Int, key: Int, firstName: String, lastName: String): dynamic {
        val r: dynamic = object {}
        r["id"] = id
        r["key"] = key
        r["firstName"] = firstName
        r["lastName"] = lastName
        return r
    }

    val rows = arrayOf(
        row(1, 1, "Peter", "K"),
        row(2, 2, "Ree", "Lo"),
        row(3, 3, "Ada", "Loq"),
        row(4, 4, "Betda", "Aoq"),
        row(5, 5, "As", "Lkk"),
        row(6, 6, "Ssa", "Soq"),
        row(7, 7, "88da", "AXZoq"),
        row(8, 8, "8da", "AXZq"),
        row(9, 9, "a", "AXZoq"),
        row(10, 10, "Asda", "AXoq")
    )


    Card {
        attrs {
            style = jso {
                width = 100.pct
                height = 1500.px
            }
        }
        DataGrid {
            attrs {
                this.columns = columns
                this.rows = rows
                pageSize = 100
                rowsPerPageOptions = arrayOf(5, 10, 100)
                columnBuffer = 8
            }
        }
    }

    div("lightgreen") {
        Divider {
            attrs {
                variant = DividerVariant.fullWidth

                Chip {
                    attrs {
                        label = ReactNode("Basic table")
                    }
                }
            }
        }
    }

    div("lightgreen") {

        if (props.content != null) {
            table {
                caption {
                    +"EMD Table component:"
                }
                thead {
                    tr {
                        th { +"Storage name" }
                        th { +"File path" }
                        th { +"Event number" }
                        th { +"Software" }
                        th { +"Period" }
                        th { +"Run" }
                        props.pageConfig.parameters.forEach { it ->
                            th {
                                +it.name
                            }
                        }
                    }
                }
                tbody {
                    //console.log(props.content)
                    val json = JSON.parse<Json>(props.content!!)
                    //console.log(json["events"])
                    val events = json["events"].unsafeCast<Array<Json>>()
                    events.forEach { event ->
                        tr {
                            // console.log(event)
                            val ref = event["reference"].unsafeCast<Json>()
                            val event_number = ref["event_number"]
                            val file_path = ref["file_path"]
                            val storage_name = ref["storage_name"]

                            td { +storage_name.toString() }
                            td { +file_path.toString() }
                            td { +event_number.toString() }
                            td { +event["software_version"].toString() }
                            td { +event["period_number"].toString() }
                            td { +event["run_number"].toString() }


                            props.pageConfig.parameters.forEach { it ->
                                val param_value = event["parameters"].unsafeCast<Json>()[it.name]
                                td { +param_value.toString() }
                            }

                        }
                    }
                }
            }

        } else {
            h3 {
                +"Empty EMD data"
            }
        }
    }
}
