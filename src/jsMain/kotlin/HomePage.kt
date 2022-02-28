import csstype.px
import kotlinext.js.jso
import mui.material.Card
import mui.material.MenuItem
import mui.material.TextField
import react.Props
import react.ReactNode
import react.dom.*
import react.fc
import react.useState
import kotlinx.html.js.onClickFunction
import kotlinx.html.DIV
import org.w3c.dom.HTMLInputElement

import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.style
import mui.material.*
import react.css.css



val homePage = fc<Props> {
    val (period, setPeriod) = useState(false);
    val (params, setParams) = useState<Map<String, String>>()
    div("home__page"){
        div(){
            div("home__page__dashboard"){
                div("home__page__dashboard__head"){
                    +"Event Metadata System"
                }
                div("home__page__dashboard__text"){
                    +"The Event Catalogue stores summary event metadata to select necessary events by criteria"
                }
            }
            div("home__page__stats"){
                div("home__page__stats__block"){
                    svg(""){ +"icon"} // <svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 504 504" style="enable-background:new 0 0 504 504;" xml:space="preserve"> <circle style="fill:#FD8469;" cx="252" cy="252" r="252"/> <rect x="94.5" y="104.3" style="fill:#324A5E;" width="315" height="295.3"/> <g> <rect x="122.1" y="170.6" style="fill:#FFFFFF;" width="259.9" height="201.5"/> <rect x="122.1" y="127.9" style="fill:#FFFFFF;" width="32.5" height="19.2"/> </g> <rect x="175.7" y="127.9" style="fill:#F9B54C;" width="32.5" height="19.2"/> <rect x="229.4" y="127.9" style="fill:#54C0EB;" width="32.5" height="19.2"/> <path style="fill:#F1543F;" d="M268.6,304.9v-26.5h-13.2c-1.2-4.5-3-8.8-5.3-12.8l9.3-9.3l-18.8-18.8l-9.3,9.3 c-4-2.3-8.2-4.1-12.8-5.3v-13.2H192v13.2c-4.5,1.2-8.8,3-12.8,5.3l-9.3-9.3l-18.8,18.8l9.3,9.3c-2.3,4-4.1,8.2-5.3,12.8h-13.2v26.5 h13.2c1.2,4.5,3,8.8,5.3,12.8l-9.3,9.3l18.8,18.8l9.3-9.3c4,2.3,8.2,4.1,12.8,5.3V355h26.5v-13.2c4.5-1.2,8.8-3,12.8-5.3l9.3,9.3 l18.8-18.8l-9.3-9.3c2.3-4,4.1-8.2,5.3-12.8L268.6,304.9L268.6,304.9z M205.3,326.3c-19.1,0-34.6-15.5-34.6-34.6 s15.5-34.6,34.6-34.6s34.6,15.5,34.6,34.6C239.8,310.8,224.4,326.3,205.3,326.3z"/> <path style="fill:#FFD05B;" d="M356.3,244.9v-17.8h-8.9c-0.8-3-2-5.9-3.6-8.6l6.3-6.3l-12.6-12.6l-6.3,6.3c-2.7-1.5-5.5-2.7-8.6-3.6 v-8.9h-17.8v8.9c-3,0.8-5.9,2-8.6,3.6l-6.3-6.3l-12.6,12.6l6.3,6.3c-1.5,2.7-2.7,5.5-3.6,8.6h-8.9v17.8h8.9c0.8,3,2,5.9,3.6,8.6 l-6.3,6.3l12.6,12.6l6.3-6.3c2.7,1.5,5.5,2.7,8.6,3.6v8.9h17.8v-8.9c3-0.8,5.9-2,8.6-3.6l6.3,6.3l12.6-12.6l-6.3-6.3 c1.5-2.7,2.7-5.5,3.6-8.6H356.3z M313.7,259.3c-12.8,0-23.3-10.4-23.3-23.3s10.4-23.3,23.3-23.3S337,223.2,337,236 S326.5,259.3,313.7,259.3z"/> </svg>            
                    div("home__page__stats__block__column"){
                        div("home__page__stats__block__column__stats"){
                            div(){
                                +"5000"
                            }
                            div{
                                +"Records"
                            }
                        }
                        div("event_metadata"){
                            +"event metadata"
                        }
                    }
                }
                div("home__page__stats__block borders stats_new_block"){
                    attrs.onClickFunction = {
                        setPeriod(!period)
                    }
                    svg(){ //<svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 504 504" style="enable-background:new 0 0 504 504;" xml:space="preserve"> <circle style="fill:#FFD05B;" cx="252" cy="252" r="252"/> <rect x="94.5" y="94.5" style="fill:#324A5E;" width="315" height="315"/> <g> <rect x="122.1" y="168.6" style="fill:#FFFFFF;" width="259.9" height="213.3"/> <rect x="122.1" y="121.9" style="fill:#FFFFFF;" width="32.5" height="19.2"/> </g> <rect x="175.7" y="121.9" style="fill:#F9B54C;" width="32.5" height="19.2"/> <rect x="229.4" y="121.9" style="fill:#54C0EB;" width="32.5" height="19.2"/> <rect x="144.1" y="188.1" style="fill:#F1543F;" width="37.4" height="23.6"/> <rect x="204.1" y="188.1" style="fill:#FF7058;" width="155.8" height="23.6"/> <rect x="322.5" y="238.4" style="fill:#84DBFF;" width="37.4" height="23.6"/> <rect x="144.1" y="238.4" style="fill:#54C0EB;" width="155.8" height="23.6"/> <rect x="144.1" y="288.6" style="fill:#FFD05B;" width="37.4" height="23.6"/> <rect x="204.1" y="288.6" style="fill:#F9B54C;" width="155.8" height="23.6"/> <rect x="322.5" y="338.8" style="fill:#4CDBC4;" width="37.4" height="23.6"/> <rect x="144.1" y="338.8" style="fill:#2C9984;" width="155.8" height="23.6"/> </svg>
                    }
                    div("stats_new_block__div"){
                        div("per"){
                            +"Period Number —"
                        }
                        div("per_number"){
                            +"7 " // из базы
                        }
                    }
                }
                if(period){
                    fun RDOMBuilder<DIV>.textSelect(paramName: String, labelString: String = ""){
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
                    div("home__page__stats__block3"){
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
                div("home__page__stats__block2"){
                    svg(){ //<svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 504 504" style="enable-background:new 0 0 504 504;" xml:space="preserve"> <circle style="fill:#324A5E;" cx="252" cy="252" r="252"/> <polygon style="fill:#FFFFFF;" points="354.4,94.5 236.6,94.5 236.6,305.7 409.5,305.7 409.5,149.6 "/> <polygon style="fill:#ACB3BA;" points="354.4,149.6 409.5,149.6 354.4,94.5 "/> <rect x="259.5" y="119.2" style="fill:#324A5E;" width="71.9" height="7.9"/> <rect x="259.5" y="141.7" style="fill:#ACB3BA;" width="71.9" height="7.9"/> <rect x="259" y="175" style="fill:#FF7058;" width="128" height="25.9"/> <rect x="259" y="216.9" style="fill:#F9B54C;" width="37.6" height="25.9"/> <rect x="304.2" y="216.9" style="fill:#2C9984;" width="37.6" height="25.9"/> <rect x="349.4" y="216.9" style="fill:#ACB3BA;" width="37" height="25.9"/> <rect x="259" y="258.8" style="fill:#54C0EB;" width="64" height="25.9"/> <rect x="331.3" y="261.5" style="fill:#ACB3BA;" width="55.1" height="6.1"/> <rect x="331.3" y="276" style="fill:#4CDBC4;" width="55.1" height="6.1"/> <path style="fill:#FFFFFF;" d="M307.9,409.5H96.1c-0.9,0-1.6-0.7-1.6-1.6V320c0-0.9,0.7-1.6,1.6-1.6h211.8c0.9,0,1.6,0.7,1.6,1.6 v87.9C309.4,408.8,308.7,409.5,307.9,409.5z"/> <g> <rect x="105.6" y="326.3" style="fill:#ACB3BA;" width="18.1" height="9.8"/> <rect x="134.7" y="326.3" style="fill:#ACB3BA;" width="18.1" height="9.8"/> <rect x="163.8" y="326.3" style="fill:#ACB3BA;" width="18.1" height="9.8"/> <rect x="192.9" y="326.3" style="fill:#ACB3BA;" width="18.1" height="9.8"/> <rect x="222" y="326.3" style="fill:#ACB3BA;" width="18.1" height="9.8"/> <rect x="251.1" y="326.3" style="fill:#ACB3BA;" width="18.1" height="9.8"/> <rect x="280.2" y="326.3" style="fill:#ACB3BA;" width="18.1" height="9.8"/> </g> <g> <rect x="105.6" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="130.7" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="155.7" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="180.8" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="205.9" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="230.9" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="256" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="281.1" y="342.8" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="105.6" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="130.7" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="155.7" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="180.8" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="205.9" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="230.9" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="256" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="281.1" y="365.1" style="fill:#CED5E0;" width="17.3" height="14.4"/> </g> <rect x="105.6" y="387.4" style="fill:#2C9984;" width="17.3" height="14.4"/> <rect x="130.7" y="387.4" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="155.7" y="387.4" style="fill:#FF7058;" width="92.5" height="14.4"/> <rect x="256" y="387.4" style="fill:#CED5E0;" width="17.3" height="14.4"/> <rect x="281.1" y="387.4" style="fill:#324A5E;" width="17.3" height="14.4"/> <path style="fill:#FFFFFF;" d="M207.9,318.5h-11.8v-12c0-5.7-4.6-10.3-10.3-10.3h-43.7c-13.9,0-25.1-11.3-25.1-25.1 c0-13.9,11.3-25.1,25.1-25.1h56.2c5.7,0,10.3-4.6,10.3-10.3s-4.6-10.3-10.3-10.3h-53.4c-14.9,0-27.1-12.1-27.1-27.1 s12.1-27.1,27.1-27.1h91.7V183h-91.7c-8.4,0-15.3,6.8-15.3,15.3s6.8,15.3,15.3,15.3h53.4c12.2,0,22.1,9.9,22.1,22.1 s-9.9,22.1-22.1,22.1h-56.2c-7.3,0-13.3,6-13.3,13.3s6,13.3,13.3,13.3h43.7c12.2,0,22.1,9.9,22.1,22.1V318.5z"/> </svg>
                    }
                    div("stats_new_block__div"){
                        div("per"){
                            +"Software Version — "
                        }
                        div("per_number"){
                            +"20.12.0 " // из базы
                        }
                    }
                }
            }
        }
        div("charts"){
            +"Charts"
        }
    }
}
