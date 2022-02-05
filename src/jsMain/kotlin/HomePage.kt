import react.Props
import react.dom.*
import react.fc

val homePage = fc<Props> {
    div("homepage-div") {
        h2 {
            +"HomePage component here"
        }
    }
}
