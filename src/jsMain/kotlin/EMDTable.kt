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
        h4 {
            +"EMD Table component:"
        }
        // p { +(props.content ?: "HZ")  }

        if (props.content != null) {
            table {
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
                        val track_number = event["parameters"].unsafeCast<Json>()["track_number"]
                        td { + event_number.toString() }
                        td { + file_path.toString() }
                        td { + storage_name.toString() }
                        td { + track_number.toString() }
                    }
                }
            }

        }
    }
}
