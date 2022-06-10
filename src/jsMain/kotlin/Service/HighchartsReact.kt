@file:JsModule("highcharts-react-official")
@file:JsNonModule

external interface HighchartsReactProps: react.Props {
    var highcharts: dynamic
    var options: dynamic
}

@JsName("default")
external val HighchartsReact: react.FC<HighchartsReactProps>
