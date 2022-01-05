import react.Props
import react.dom.*
import react.fc

external interface EMDTableProps: Props {
    // TODO make structured
    var content: String
}

val EMDTable = fc<EMDTableProps> { props ->
    div("lightgreen") {
        h4 {
            + "EMD Table component:"
        }
        p {
            + props.content
        }
    }
}
