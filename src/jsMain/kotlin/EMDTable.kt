import react.Props
import react.dom.*
import react.fc
import kotlin.js.Json

external interface EMDTableProps : Props {
    // TODO make structured
    var content: String?
    var pageConfig: PageConfig
}

val EMDTable = fc<EMDTableProps> { props ->
    div("lightgreen") {

        if (props.content != null) {
            table {
                caption {
                    +"EMD Table component:"
                }
                thead {
                    tr {
                        th { +"Event number" }
                        th { +"File path" }
                        th { +"Storage name" }
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

                            td { +event_number.toString() }
                            td { +file_path.toString() }
                            td { +storage_name.toString() }

                            props.pageConfig.parameters.forEach { it ->
                                val param_value = event["parameters"].unsafeCast<Json>()[it.name]
                                td { +param_value.toString() }
                            }

                        }
                    }
                }
            }

        }
    }
}
