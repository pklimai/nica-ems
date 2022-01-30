import react.*
import react.dom.*
import kotlinx.html.js.*
import kotlinx.coroutines.*


val scope = MainScope()

val app = fc<Props> { props ->

    val (config, setConfig) = useState<ConfigFile>()

    val (currentPage, setCurrentPage) = useState<PageConfig>()
    // setCurrentPage(null) -- valid but causes too many re-renders here!

    val (EMDdata, setEMDdata) = useState<String>()


    useEffectOnce {
        scope.launch {
            setConfig(getConfig())
        }
    }

    // kotlin-react-dom-legacy is used here
    div("lightblue") {
        h1 {
            +(config?.title ?: "NOT LOADED")
        }
        ul {
            config?.pages?.forEach { item ->
                li {
                    key = item.name
                    attrs.onClickFunction = {
                        setCurrentPage(item)
                        // Clear data for table
                        setEMDdata(null)
                    }
                    +"[${item.name}] ${item.api_url} "
                }
            }

            li {
                key = "Home"
                attrs.onClickFunction = {
                    setCurrentPage(null)
                }
                +"Home"
            }

        }

        div {
            if (currentPage == null) {
                child(homePage)
            } else {
                child(emdPage) {
                    attrs.pageConfig = currentPage
                    attrs.EMDdata = EMDdata
                    attrs.setEMDdata = { it: String ->
                        setEMDdata(it)
                    }
                }
            }

        }
    }

}

