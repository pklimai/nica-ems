package ru.mipt.npm.nica.emd

import Highcharts
import HighchartsReact
import kotlinext.js.jso
import kotlinx.html.DIV
import kotlinx.html.js.onClickFunction
import mui.material.FormControlVariant
import mui.material.TextField
import react.*
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.onChange
import kotlin.js.json


val homePage = fc<Props> {
    val (period, setPeriod) = useState(false);
    val (params, setParams) = useState<Map<String, String>>()
    div("home__page") {
        div() {
            div("home__page__dashboard") {
                div("home__page__dashboard__head") {
                    +"Event Metadata System"
                }
                div("home__page__dashboard__text") {
                    +"The Event Catalogue stores summary event metadata to select necessary events by criteria"
                }
            }
            div("home__page__stats") {
                div("home__page__stats__block") {
                    dangerousSVG(SVGHomeRecords)
                    div("home__page__stats__block__column") {
                        div("home__page__stats__block__column__stats") {
                            div() {
                                +"5000"
                            }
                            div {
                                +"Records"
                            }
                        }
                        div("event_metadata") {
                            +"event metadata"
                        }
                    }
                }
                div("home__page__stats__block borders stats_new_block") {
                    attrs.onClickFunction = {
                        setPeriod(!period)
                    }
                    dangerousSVG(SVGHomePeriod)
                    div("stats_new_block__div") {
                        div("per") {
                            +"Period Number —"
                        }
                        div("per_number") {
                            +"7 " // из базы
                        }
                    }
                }
                if (period) {
                    fun RDOMBuilder<DIV>.textSelect(paramName: String, labelString: String = "") {
                        TextField {
                            attrs {
                                name = paramName
                                id = paramName
                                value = params?.get(paramName) ?: ""    /// ? to test
                                variant = FormControlVariant.standard
                                label = ReactNode(labelString)
                                onChange = { }
                            }
                        }
                    }
                    div("home__page__stats__block3") {
                        textSelect("period number", "Period Number")
                    }
                }
                /*
                    <div *ngIf="per" class="home__page__stats__block3" style="box-shadow: rgba(100, 100, 111, 0.2) 0px 7px 29px 0px;">
                        <mat-form-field class="wform" style="margin-bottom: -10%;width: 110px;">
                            <mat-select  placeholder="Period number" [(ngModel)]="fil.per" [ngModelOptions]="{standalone: true}">
                                <mat-option  *ngFor="let per of data;"  [value]="per" (click)="start();">Period {{per}} </mat-option>
                            </mat-select>
                        </mat-form-field>
                    </div>
                */
                div("home__page__stats__block2") {
                    dangerousSVG(SVGHomeSoftware)
                    div("stats_new_block__div") {
                        div("per") {
                            +"Software Version — "
                        }
                        div("per_number") {
                            +"20.12.0 " // из базы
                        }
                    }
                }
            }
        }
        div("charts") {
            +"Charts"
            div {
                HighchartsReact {
                    attrs {
                        this.highcharts = Highcharts
                        this.options = json(
                            "title" to "",
                            "chart" to js("{borderWidth: 0, plotShadow: false, margin: [0, 0, 0, 8], spacingTop: 0, spacingBottom: 0, spacingLeft: 0, spacingRight: 0, style: { fontFamily: 'Arial, Helvetica, Clean, sans-serif'} }"),
                            "credits" to json(
                                "enabled" to false
                            ),
                            "tooltip" to js("{style: { color: '#1b1818' }, backgroundColor: '#ffffff', borderRadius: 5, borderWidth: 3, headerFormat: '<small></small>', pointFormat: '{point.name}:  {point.y} MEvents' }"),
                            "plotOptions" to json(
                                "pie" to json(
                                    "allowPointSelect" to true,
                                    "cursor" to "pointer",
                                    "startAngle" to -120,
                                    "center" to arrayOf("50%", "50%"),
                                    "size" to "51%",
                                    "depth" to 15,
                                    "colors" to arrayOf("#ff0000", "#00ff00", "#0000ff"),  // this.bname[i].color,
                                    "dataLabels" to json(
                                        "enabled" to true,
                                        "distance" to 15,
                                        "style" to js("{ fontSize: 'inherit', fontWeight: 'normal', fontFamily: 'Lato, sans-serif', lineHeight: '18px' }"),
                                        "format" to "{point.name}: <i>{point.y} MEvents</i>"
                                    )

                               )
                            ),
                            "series" to arrayOf<dynamic>(
                                json(
                                    "type" to "pie",
                                    "name" to "",
                                    "data" to arrayOf(1, 2, 3, 4, 5)
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
