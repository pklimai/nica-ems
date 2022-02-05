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


    data class Column(val field: String, val key: Int, val headerName: String, val width: Int)

    val columns = arrayOf(
        Column("id", 1, "ID", 90),
        Column("firstName", 2, "First name", 150),
        Column("lastName", 3, "Last name", 150)
    )

    @Serializable
    data class Row(val id: Int, val key: Int, val firstName: String, val lastName: String)

    val rows = arrayOf(
        Row(1, 1, "Peter", "K"),
        Row(2, 2, "Ree", "Lo"),
        Row(3, 3, "Ada", "Loq"),
        Row(4, 4, "Betda", "Aoq"),
        Row(5, 5, "As", "Lkk"),
        Row(6, 6, "Ssa", "Soq"),
        Row(7, 7, "88da", "AXZoq"),
        Row(8, 8, "8da", "AXZq"),
        Row(9, 9, "a", "AXZoq"),
        Row(10, 10, "Asda", "AXoq")
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
