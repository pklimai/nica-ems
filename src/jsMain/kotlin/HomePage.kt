import csstype.px
import kotlinext.js.jso
import mui.material.Card
import react.Props
import react.dom.*
import react.fc

val homePage = fc<Props> {
    div("homepage-div") {
        Card {
            attrs {
                style = jso {
                    paddingTop = 10.px
                    paddingLeft = 25.px
                    height = 600.px
                }
            }
            h2 {
                +"Home Page goes here"
            }
            + "Enjoy our EMS!"
        }
    }
}
