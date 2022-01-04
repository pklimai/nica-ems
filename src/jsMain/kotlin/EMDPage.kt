import react.Props
import react.dom.*
import react.fc

// Component to render attribute selection and nested table


external interface EMDPageProps : Props {
    var pageConfig: PageConfig
}

val emdPage = fc<EMDPageProps> { props ->
    div("yellow") {

        h3 {
            +"EMDPage component"
        }
        h3 {
            +props.pageConfig.name
        }
    }

}